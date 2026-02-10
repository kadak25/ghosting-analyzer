import { useState } from "react";
import api from "../lib/api";
import { AuthShell, Button, ErrorBanner, Input, Label } from "../ui/components";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim() || !password) return;

    try {
      setLoading(true);
      setError("");


      const res = await api.post("/api/auth/login", {
        email: email.trim(),
        password,
      });


      const token = res.data?.accessToken || res.data?.token;
      if (!token) {
        setError("Token alınamadı (backend response farklı).");
        return;
      }

      localStorage.setItem("accessToken", token);
      location.href = "/";
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || "Login failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      title="Login"
      subtitle="Hesabına giriş yap"
      rightHint={
        <div style={{ opacity: 0.75 }}>
          Hesabın yoksa <a href="/register" style={{ color: "#8ab4ff" }}>Register</a>
        </div>
      }
    >
      <ErrorBanner message={error} />

      <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        <div>
          <Label>Email</Label>
          <Input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="kadak@example.com"
            autoComplete="email"
          />
        </div>

        <div>
          <Label>Password</Label>
          <Input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            type="password"
            autoComplete="current-password"
          />
        </div>

        <div style={{ display: "flex", gap: 10, marginTop: 6 }}>
          <Button type="submit" disabled={loading || !email.trim() || !password}>
            {loading ? "Logging in..." : "Login"}
          </Button>

          <Button
            variant="ghost"
            onClick={() => (location.href = "/register")}
            disabled={loading}
          >
            Register →
          </Button>
        </div>
      </form>
    </AuthShell>
  );
}
