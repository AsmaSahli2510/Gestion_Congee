
import React, { useState } from "react";
import { Link } from "react-router-dom";
import AuthLayout from "../components/AuthLayout";
import AuthFields from "../components/AuthFields";
import { forgotPasswordRequest } from "../services/authService";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setSent(false);
    setLoading(true);

    try {
      await forgotPasswordRequest(email.trim());
      setSent(true);
    } catch (err) {
      setError(
        err.message || "Une erreur est survenue. Réessayez plus tard."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <AuthLayout>
      <Link className="back" to="/login">
        ← Retour à la connexion
      </Link>

      <h2>Retrouvons votre accès.</h2>
      <p className="sub">
        Renseignez l’email associé à votre compte.
      </p>

      <form onSubmit={handleSubmit}>
        <AuthFields
          password={false}
          email={email}
          setEmail={setEmail}
        />

        <button
          className="submit"
          type="submit"
          disabled={loading || !email.trim()}
        >
          {loading ? "Envoi en cours..." : "Recevoir un lien →"}
        </button>
      </form>

      {sent && (
        <div className="message" role="status">
          Si un compte correspond à cette adresse, vous recevrez
          les instructions de réinitialisation.
        </div>
      )}

      {error && (
        <div className="message" role="alert">
          {error}
        </div>
      )}
    </AuthLayout>
  );
}
