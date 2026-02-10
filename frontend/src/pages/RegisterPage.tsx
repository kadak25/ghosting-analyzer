import { useState } from "react";
import api from "../lib/api";
import { AuthShell, Button, ErrorBanner, Input, Label } from "../ui/components";

export default function RegisterPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim() || password.length < 6) return;

    try {
      setLoading(true);
      setError("");


      await api.post("/api/auth/register", {
        name: name.trim(),
        email: email.trim(),
        password,
      });


      location.href = "/login";
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || "Register failed");
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthShell
      title="Register"
      subtitle="Yeni hesap oluştur"
      rightHint={
        <div style={{ opacity: 0.75 }}>
          Zaten hesabın var mı? <a href="/login" style={{ color: "#8ab4ff" }}>Login</a>
        </div>
      }
    >
      <ErrorBanner message={error} />

      <form onSubmit={submit} style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        <div>
          <Label>Name (opsiyonel)</Label>
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Mustafa"
            autoComplete="name"
          />
        </div>

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
          <Label>Password (min 6)</Label>
          <Input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            type="password"
            autoComplete="new-password"
          />
        </div>

        <div style={{ display: "flex", gap: 10, marginTop: 6 }}>
          <Button type="submit" disabled={loading || !email.trim() || password.length < 6}>
            {loading ? "Creating..." : "Create account"}
          </Button>

          <Button variant="ghost" onClick={() => (location.href = "/login")} disabled={loading}>
            Login →
          </Button>
        </div>
      </form>
    </AuthShell>
  );
}
