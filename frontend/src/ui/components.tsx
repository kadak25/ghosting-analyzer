import React, { useEffect } from "react";
import { theme } from "./theme";

function useRuntimePageFixes() {
  useEffect(() => {

    const html = document.documentElement;
    const body = document.body;

    const prevHtml = {
      overflowX: html.style.overflowX,
      background: html.style.background,
    };
    const prevBody = {
      margin: body.style.margin,
      background: body.style.background,
      overflowX: body.style.overflowX,
    };


    html.style.overflowX = "hidden";
    html.style.background = theme.bg;

    body.style.margin = "0";
    body.style.background = theme.bg;
    body.style.overflowX = "hidden";

    return () => {
      html.style.overflowX = prevHtml.overflowX;
      html.style.background = prevHtml.background;

      body.style.margin = prevBody.margin;
      body.style.background = prevBody.background;
      body.style.overflowX = prevBody.overflowX;
    };
  }, []);
}

export function Page({ children }: { children: React.ReactNode }) {
  useRuntimePageFixes();

  return (
    <div
      style={{
        minHeight: "100vh",
        width: "100%",
        padding: 28,
        background: theme.bg,
        color: "white",
        overflowX: "hidden",
        boxSizing: "border-box",
      }}
    >
      {children}
    </div>
  );
}

export function Container({ children }: { children: React.ReactNode }) {
  return (
    <div
      style={{
        maxWidth: 1240,
        margin: "0 auto",
        width: "100%",
        boxSizing: "border-box",
      }}
    >
      {children}
    </div>
  );
}

export function TopBar({
  title,
  subtitle,
  right,
}: {
  title: string;
  subtitle?: string;
  right?: React.ReactNode;
}) {
  return (
    <div
      style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        gap: 12,
        marginBottom: 18,
        flexWrap: "wrap",
      }}
    >
      <div style={{ display: "flex", flexDirection: "column", gap: 6, minWidth: 0 }}>
        <h1 style={{ margin: 0, fontSize: 36, letterSpacing: 0.2 }}> {title}</h1>
        {subtitle && <div style={{ opacity: 0.7, fontSize: 13 }}>{subtitle}</div>}
      </div>
      <div style={{ display: "flex", gap: 10, alignItems: "center", flexWrap: "wrap" }}>
        {right}
      </div>
    </div>
  );
}

export function Card({
  title,
  subtitle,
  children,
  style,
}: {
  title?: string;
  subtitle?: string;
  children: React.ReactNode;
  style?: React.CSSProperties;
}) {
  return (
    <div
      style={{
        border: `1px solid ${theme.border}`,
        borderRadius: 18,
        padding: 18,
        background: theme.panel,
        boxShadow: theme.shadow,
        boxSizing: "border-box",
        minWidth: 0,
        ...style,
      }}
    >
      {title && <h2 style={{ margin: 0, fontSize: 20 }}>{title}</h2>}
      {subtitle && <div style={{ opacity: 0.7, fontSize: 12, marginTop: 6 }}>{subtitle}</div>}
      {(title || subtitle) && (
        <div style={{ height: 1, background: "rgba(255,255,255,0.06)", margin: "14px 0" }} />
      )}
      {children}
    </div>
  );
}

export function ErrorBanner({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <div
      style={{
        border: `1px solid ${theme.dangerBorder}`,
        borderRadius: 18,
        padding: 16,
        background: theme.dangerBg,
        marginBottom: 16,
        boxShadow: theme.shadow,
        boxSizing: "border-box",
        minWidth: 0,
      }}
    >
      <b>Hata:</b> {message}
    </div>
  );
}

export function Button({
  children,
  onClick,
  disabled,
  variant = "primary",
  type,
  style,
}: {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  variant?: "primary" | "danger" | "ghost";
  type?: "button" | "submit";
  style?: React.CSSProperties;
}) {
  const base: React.CSSProperties = {
    padding: "10px 14px",
    borderRadius: 12,
    border: `1px solid ${theme.border}`,
    background: theme.buttonBg,
    color: "white",
    cursor: disabled ? "not-allowed" : "pointer",
    opacity: disabled ? 0.6 : 1,
    transition: "120ms",
    boxSizing: "border-box",
    maxWidth: "100%",
  };

  const variants: Record<string, React.CSSProperties> = {
    primary: { background: theme.buttonBg },
    danger: { background: "#140a12", borderColor: "#5a2330", color: "#ffd6dc" },
    ghost: { background: "transparent" },
  };

  return (
    <button
      type={type ?? "button"}
      onClick={onClick}
      disabled={disabled}
      style={{ ...base, ...variants[variant], ...style }}
    >
      {children}
    </button>
  );
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      style={{
        width: "100%",
        maxWidth: "100%",
        padding: 12,
        borderRadius: 12,
        border: `1px solid ${theme.border}`,
        background: theme.inputBg,
        color: "white",
        outline: "none",
        boxSizing: "border-box",
        minWidth: 0,
        ...(props.style as any),
      }}
    />
  );
}

export function Textarea(props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return (
    <textarea
      {...props}
      style={{
        width: "100%",
        maxWidth: "100%",
        padding: 12,
        borderRadius: 12,
        border: `1px solid ${theme.border}`,
        background: theme.inputBg,
        color: "white",
        outline: "none",
        minHeight: 160,
        resize: "vertical",
        boxSizing: "border-box",
        minWidth: 0,
        overflowX: "hidden",
        wordBreak: "break-word",
        ...(props.style as any),
      }}
    />
  );
}

export function Label({ children }: { children: React.ReactNode }) {
  return <div style={{ opacity: 0.8, fontSize: 12, marginBottom: 6 }}>{children}</div>;
}

export function TwoCol({ left, right }: { left: React.ReactNode; right: React.ReactNode }) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "1.05fr 0.95fr",
        gap: 18,
        alignItems: "start",
        width: "100%",
        minWidth: 0,
      }}
    >

      <div style={{ display: "flex", flexDirection: "column", gap: 18, minWidth: 0 }}>{left}</div>
      <div style={{ display: "flex", flexDirection: "column", gap: 18, minWidth: 0 }}>{right}</div>
    </div>
  );
}

export function AuthShell({
  title,
  subtitle,
  children,
  rightHint,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
  rightHint?: React.ReactNode;
}) {
  return (
    <Page>
      <Container>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 18,
            alignItems: "start",
            width: "100%",
            minWidth: 0,
          }}
        >
          <Card title={title} subtitle={subtitle} style={{ minHeight: 420, minWidth: 0 }}>
            {children}
          </Card>

          <Card title="Ghosting Analyzer" subtitle="Red gelmedi ama elendin. Neden?" style={{ minWidth: 0 }}>
            <div style={{ opacity: 0.75, lineHeight: 1.6, minWidth: 0 }}>
              <div style={{ fontWeight: 800, marginBottom: 8 }}>Ne yapıyor?</div>
              <ul style={{ marginTop: 0, paddingLeft: 18 }}>
                <li>CV + ilan → match score</li>
                <li>ATS okunabilirlik</li>
                <li>Muhtemel elenme sebepleri</li>
                <li>Bir sonraki başvuruda yapılacaklar</li>
              </ul>
              {rightHint ? <div style={{ marginTop: 12 }}>{rightHint}</div> : null}
              <div style={{ marginTop: 14, opacity: 0.65, fontSize: 12 }}>
                MVP UI — sizin AutoFlow/Panel tarzı.
              </div>
            </div>
          </Card>
        </div>

        {/* ✅ Responsive: dar ekranda tek kolona düşsün  */}
        <style>
          {`
            @media (max-width: 980px) {
              .__authGrid { grid-template-columns: 1fr !important; }
            }
          `}
        </style>
      </Container>
    </Page>
  );
}
