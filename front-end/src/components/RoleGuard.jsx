
import React from "react";
import { Navigate, Outlet } from "react-router-dom";
import { useApp } from "../context/AppContext";

export default function RoleGuard({ role }) {
  const { user, authLoading } = useApp();

  // Attendre la vérification de la session
  if (authLoading) {
    return <div className="loading">Chargement de votre espace...</div>;
  }

  // Si aucune session n'existe, retourner à la connexion
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Normaliser le rôle pour éviter les différences de majuscules
  const userRole = String(user.role || "").trim().toLowerCase();

  // Rediriger vers le bon espace selon le rôle
  const dashboardRoutes = {
    rh: "/rh/dashboard",
    manager: "/manager/dashboard",
    employe: "/employe/dashboard",
  };

  if (userRole !== role) {
    return (
      <Navigate
        to={dashboardRoutes[userRole] || "/login"}
        replace
      />
    );
  }

  return <Outlet />;
}
