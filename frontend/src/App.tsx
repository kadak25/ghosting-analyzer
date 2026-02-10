import { Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import DashboardPage from "./pages/DashboardPage";

const authed = () => (localStorage.getItem("accessToken") || "").trim().length > 0;


export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={authed() ? <DashboardPage /> : <Navigate to="/login" />} />
    </Routes>
  );
}
