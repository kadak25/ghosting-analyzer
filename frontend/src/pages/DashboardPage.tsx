import { useEffect, useState } from "react";
import api from "../lib/api";
import {
  Page,
  Container,
  TopBar,
  Card,
  Button,
  Input,
  Textarea,
  Label,
  TwoCol,
  ErrorBanner,
} from "../ui/components";

/* ========= TYPES ========= */

type CvSummary = {
  cvId: string;
  filename: string;
  createdAt: string;
};

type AnalyzeResponse = {
  analysisId: string;
  cvId: string;
  resultJson: string;
  createdAt: string;
};

/* ========= HELPERS ========= */

function parseResult(json: string) {
  try {
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function formatAnalysisTitle(index: number) {
  return `Analiz #${index + 1}`;
}

/* ========= PAGE ========= */

export default function DashboardPage() {
  const [cvs, setCvs] = useState<CvSummary[]>([]);
  const [selectedCvId, setSelectedCvId] = useState("");
  const [jobDescription, setJobDescription] = useState("");

  const [result, setResult] = useState<any>(null);
  const [history, setHistory] = useState<AnalyzeResponse[]>([]);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  /* ===== LOAD DATA ===== */

  useEffect(() => {
    loadCvs();
    loadHistory();
  }, []);

  async function loadCvs() {
    try {
      const res = await api.get("/api/cvs");
      setCvs(res.data);
      if (res.data.length > 0) {
        setSelectedCvId(res.data[0].cvId);
      }
    } catch (e: any) {
      setError("CV listesi alınamadı");
    }
  }

  async function loadHistory() {
    try {
      const res = await api.get("/api/analyses");
      setHistory(res.data);
    } catch {
      /* ignore */
    }
  }

  /* ===== ACTIONS ===== */

  async function uploadCv(file: File) {
    const form = new FormData();
    form.append("file", file);

    try {
      setLoading(true);
      await api.post("/api/cvs", form);
      await loadCvs();
    } catch {
      setError("CV yüklenemedi");
    } finally {
      setLoading(false);
    }
  }

  async function analyze() {
    if (!selectedCvId || !jobDescription.trim()) return;

    try {
      setLoading(true);
      setError("");

      const res = await api.post("/api/analyses", {
        cvId: selectedCvId,
        jobDescription,
      });

      const parsed = parseResult(res.data.resultJson);
      setResult(parsed);

      await loadHistory();
    } catch (e: any) {
      setError("Analyze başarısız");
    } finally {
      setLoading(false);
    }
  }

  /* ===== RENDER ===== */

  return (
    <Page>
      <Container>
        <TopBar
          title="Ghosting Analyzer"
          subtitle="CV yükle → Job description yapıştır → Analyze → Sonucu gör"
          right={
            <Button
              variant="ghost"
              onClick={() => {
                localStorage.removeItem("accessToken");
                location.href = "/login";
              }}
            >
              Logout
            </Button>
          }
        />

        <ErrorBanner message={error} />

        <TwoCol
          left={
            <>
              {/* ===== CV UPLOAD ===== */}
              <Card title="1) CV Upload">
                <input
                  type="file"
                  accept=".pdf,.doc,.docx"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) uploadCv(f);
                  }}
                />
                <div style={{ opacity: 0.7, fontSize: 12, marginTop: 8 }}>
                  {cvs.length} CV bulundu
                </div>
              </Card>

              {/* ===== ANALYZE ===== */}
              <Card title="2) Analyze">
                <div style={{ marginBottom: 12 }}>
                  <Label>CV seç</Label>
                  <select
                    value={selectedCvId}
                    onChange={(e) => setSelectedCvId(e.target.value)}
                    style={{
                      width: "100%",
                      padding: 12,
                      borderRadius: 12,
                      background: "#0b1220",
                      color: "white",
                      border: "1px solid #243046",
                    }}
                  >
                    {cvs.map((cv) => (
                      <option key={cv.cvId} value={cv.cvId}>
                        {cv.filename} —{" "}
                        {new Date(cv.createdAt).toLocaleString()}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <Label>Job description</Label>
                  <Textarea
                    value={jobDescription}
                    onChange={(e) => setJobDescription(e.target.value)}
                    placeholder="İlan metnini buraya yapıştır"
                  />
                  <div
                    style={{
                      textAlign: "right",
                      fontSize: 11,
                      opacity: 0.6,
                      marginTop: 4,
                    }}
                  >
                    {jobDescription.length} karakter
                  </div>
                </div>

                <Button
                  style={{ marginTop: 12 }}
                  onClick={analyze}
                  disabled={loading}
                >
                  {loading ? "Analyzing..." : "Analyze"}
                </Button>
              </Card>
            </>
          }
          right={
            <>
              {/* ===== RESULT ===== */}
              <Card title="Result">
                {!result && (
                  <div style={{ opacity: 0.6 }}>
                    Henüz analiz yapılmadı
                  </div>
                )}

                {result && (
                  <>
                    {/* METRICS */}
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "1fr 1fr 1fr",
                        gap: 12,
                        marginBottom: 14,
                      }}
                    >
                      <Card>
                        Ghosting ihtimali
                        <div style={{ fontSize: 28, fontWeight: 800 }}>
                          %{Math.round(result.ghosting_probability * 100)}
                        </div>
                      </Card>
                      <Card>
                        Match score
                        <div style={{ fontSize: 28, fontWeight: 800 }}>
                          {result.match_score}
                        </div>
                      </Card>
                      <Card>
                        ATS Readability
                        <div style={{ fontSize: 28, fontWeight: 800 }}>
                          {result.ats_readability_score}
                        </div>
                      </Card>
                    </div>

                    <div style={{ opacity: 0.8, marginBottom: 12 }}>
                      Rol: <b>{result.role_guess}</b> | Seviye:{" "}
                      <b>{result.seniority_guess}</b>
                    </div>

                    {/* MISSING */}
                    <Section
                      title="Eksik Skill’ler"
                      items={result.missing_skills}
                    />

                    {/* REASONS */}
                    <Section
                      title="Muhtemel Red Nedenleri"
                      items={result.top_rejection_reasons?.map(
                        (r: any) =>
                          `${r.reason} (%${Math.round(
                            r.confidence * 100
                          )})`
                      )}
                    />

                    {/* FIXES */}
                    <Section
                      title="Önerilen Düzeltmeler"
                      items={result.fixes?.map(
                        (f: any) => `${f.area}: ${f.action}`
                      )}
                    />

                    {/* REWRITE */}
                    <Section
                      title="Rewrite Önerileri"
                      items={result.rewrite_suggestions?.map(
                        (r: any) =>
                          `${r.original} → ${r.improved}`
                      )}
                    />
                  </>
                )}
              </Card>
            </>
          }
        />

        {/* ===== HISTORY ===== */}
        <Card title="History" style={{ marginTop: 18 }}>
          {history.length === 0 && (
            <div style={{ opacity: 0.6 }}>Henüz analiz yok</div>
          )}

          {history.map((h, i) => (
            <div
              key={h.analysisId}
              onClick={() => setResult(parseResult(h.resultJson))}
              style={{
                padding: 12,
                borderRadius: 12,
                border: "1px solid #243046",
                marginBottom: 8,
                cursor: "pointer",
                background: "#0c1426",
              }}
            >
              <b>{formatAnalysisTitle(i)}</b>
              <div style={{ opacity: 0.6, fontSize: 12 }}>
                {new Date(h.createdAt).toLocaleString()}
              </div>
            </div>
          ))}
        </Card>
      </Container>
    </Page>
  );
}

/* ========= SMALL SECTION ========= */

function Section({ title, items }: { title: string; items?: string[] }) {
  if (!items || items.length === 0) return null;
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ fontWeight: 700, marginBottom: 6 }}>{title}</div>
      <ul style={{ margin: 0, paddingLeft: 18 }}>
        {items.map((i, idx) => (
          <li key={idx}>{i}</li>
        ))}
      </ul>
    </div>
  );
}
