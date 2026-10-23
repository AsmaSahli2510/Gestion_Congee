
import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import AuthLayout from "../components/AuthLayout";
import AuthFields from "../components/AuthFields";
import { useApp } from "../context/AppContext";
import {
  loginRequest,
  getCurrentUser,
} from "../services/authService";

export default function Login() {
  const { login } = useApp();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  
async function handleSubmit(event) {
  event.preventDefault();
  setError("");
  setLoading(true);

  try {
    const result = await loginRequest(email.trim(), password);

    if (!result.success) {
      throw new Error(result.message || "Connexion impossible.");
    }

    const session = await getCurrentUser();

    if (!session.authenticated || !session.user) {
      throw new Error("Impossible de vérifier votre session.");
    }

    const role = String(
      session.role || result.role || session.user.role || ""
    ).trim().toLowerCase();

    const routes = {
      rh: "/rh/dashboard",
      manager: "/manager/dashboard",
      employe: "/employe/dashboard",
    };

    if (!routes[role]) {
      throw new Error(
        `Rôle non reconnu : "${role || "vide"}". Vérifiez le rôle dans PostgreSQL.`
      );
    }

    const user = {
      ...session.user,
      role,
      balance: session.balance ?? result.balance,
      company: session.company ?? null,
    };

    login(user);
    navigate(routes[role], { replace: true });
  } catch (err) {
    setError(err.message || "Impossible de contacter le serveur.");
  } finally {
    setLoading(false);
  }
}


  return (
    <AuthLayout>
      <h2>Ravi de vous retrouver.</h2>

      <p className="sub">
        Connectez-vous à votre espace professionnel.
      </p>

      <form onSubmit={handleSubmit}>
        <AuthFields
          email={email}
          setEmail={setEmail}
          passwordValue={password}
          setPasswordValue={setPassword}
        />

        <div className="options">
          <label className="remember">
            <input type="checkbox" />
            Se souvenir de moi
          </label>

          <Link to="/mot-de-passe-oublie">
            Mot de passe oublié ?
          </Link>
        </div>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <button
          className="submit"
          type="submit"
          disabled={loading}
        >
          {loading ? "Connexion..." : "Se connecter"}
          {!loading && <span> →</span>}
        </button>
      </form>

      <div className="divider">NOUVEAU SUR LEAVEFLOW ?</div>

      <Link
        className="create"
        style={{ display: "block", textAlign: "center" }}
        to="/signup"
      >
        Créer un espace entreprise ↗
      </Link>

      <p className="invitation">
        Manager ou employé ? Rejoignez votre équipe sur invitation.
      </p>
    </AuthLayout>
  );
}
