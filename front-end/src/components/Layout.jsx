
import React, { useState } from "react";
import {
  Navigate,
  NavLink,
  Outlet,
  useNavigate,
} from "react-router-dom";

import {
  LayoutDashboard,
  Users,
  CalendarDays,
  ClipboardList,
  Wallet,
  BarChart3,
  Bell,
  LogOut,
  Menu,
  Building2,
  Mail,
  UserRound,
  History,
  SlidersHorizontal,
  ChevronDown,
} from "lucide-react";

import { useApp } from "../context/AppContext";
import { Avatar } from "./UI";
import logo from "../images/logo.png";

export default function Layout() {
  const { user, logout, authLoading } = useApp();

  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  // Attendre la vérification de la session
  if (authLoading) {
    return (
      <div className="loading">
        Chargement de votre espace...
      </div>
    );
  }

  // Si l'utilisateur n'est pas connecté
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Normaliser le rôle
  const role = String(user.role || "").trim().toLowerCase();
  const root = "/" + role;

  // Informations utilisateur
  const userName =
    user.prenom || user.nom
      ? `${user.prenom || ""} ${user.nom || ""}`.trim()
      : user.name || user.email || "Utilisateur";

  // Informations entreprise
  const companyName =
    user.company?.nom ||
    user.company?.name ||
    "Mon entreprise";

  // Notifications disponibles dans le contexte utilisateur
  const notifications = Array.isArray(user.notifications)
    ? user.notifications
    : [];

  const count = notifications.filter(
    (notification) =>
      String(notification.userId) === String(user.id) &&
      !notification.read
  ).length;

  // Menu de navigation
  const links = [
    ["dashboard", "Vue d’ensemble", LayoutDashboard],
  ];

  if (role === "rh") {
    links.push(
      ["entreprise", "Mon entreprise", Building2],
      ["equipes", "Équipes & managers", Users],
      ["employes", "Employés", UserRound],
      ["invitations", "Invitations", Mail]
    );
  }

  if (role === "manager") {
    links.push(["equipe", "Mon équipe", Users]);
  }

  if (role !== "employe") {
    links.push([
      "validations",
      "Demandes à valider",
      ClipboardList,
    ]);
  }

  links.push(
    ["demandes", "Mes demandes", ClipboardList],
    ["calendrier", "Calendrier", CalendarDays],
    [
      "soldes",
      role === "employe" ? "Mon solde" : "Soldes de congés",
      Wallet,
    ]
  );

  if (role !== "employe") {
    links.push(["statistiques", "Statistiques", BarChart3]);
  }

  links.push(
    ["historique", "Historique", History],
    ["notifications", "Notifications", Bell],
    ["profil", "Mon profil", UserRound]
  );

  if (role === "rh") {
    links.push([
      "parametres",
      "Règles & paramètres",
      SlidersHorizontal,
    ]);
  }

  // Déconnexion
  async function handleLogout() {
    try {
      await logout();
    } finally {
      navigate("/login", { replace: true });
    }
  }

  return (
    <div className="app-shell">
      {/* Barre latérale */}
      <aside className={open ? "sidebar open" : "sidebar"}>
        <NavLink
          to={root + "/dashboard"}
          onClick={() => setOpen(false)}
        >
          <img
            className="brand"
            src={logo}
            alt="LeaveFlow"
          />
        </NavLink>

        {/* Entreprise */}
        <div className="workspace">
          <span className="workspace-icon">N</span>

          <div>
            <strong>{companyName}</strong>

            <small>
              Espace{" "}
              {role === "rh"
                ? "Ressources humaines"
                : role === "manager"
                ? "Manager"
                : "Employé"}
            </small>
          </div>

          <ChevronDown size={14} />
        </div>

        <div className="nav-label">
          ESPACE DE TRAVAIL
        </div>

        {/* Navigation */}
        <nav>
          {links.map(([url, label, Icon]) => (
            <NavLink
              key={url}
              to={root + "/" + url}
              onClick={() => setOpen(false)}
            >
              <Icon size={18} />

              <span>{label}</span>

              {url === "notifications" && count > 0 && (
                <b className="count">{count}</b>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Bas du menu */}
        <div className="sidebar-bottom">
          <div className="demo-tag">
            LEAVEFLOW · GESTION DES CONGÉS
          </div>

          <button onClick={handleLogout}>
            <LogOut size={17} />
            Déconnexion
          </button>
        </div>
      </aside>

      {/* Fond du menu mobile */}
      {open && (
        <button
          className="scrim"
          aria-label="Fermer le menu"
          onClick={() => setOpen(false)}
        />
      )}

      {/* Contenu principal */}
      <div className="app-main">
        <header className="topbar">
          <button
            className="icon-btn mobile-menu"
            aria-label="Ouvrir le menu"
            onClick={() => setOpen(!open)}
          >
            <Menu />
          </button>

          <span className="breadcrumb">
            Workspace <span>/</span> {companyName}
          </span>

          <div className="top-actions">
            <span className="today">
              {new Intl.DateTimeFormat("fr-FR", {
                day: "numeric",
                month: "long",
                year: "numeric",
              }).format(new Date())}
            </span>

            <NavLink
              className="bell"
              to={root + "/notifications"}
              aria-label="Notifications"
            >
              <Bell size={19} />

              {count > 0 && <i />}
            </NavLink>

            <span className="top-divider" />

            <Avatar name={userName} />

            <div className="user-info">
              <strong>{userName}</strong>

              <small>
                {role === "rh"
                  ? "Responsable RH"
                  : role === "manager"
                  ? "Manager"
                  : "Employé"}
              </small>
            </div>
          </div>
        </header>

        {/* Pages : dashboard, demandes, calendrier, etc. */}
        <main className="page-content">
          <Outlet />
        </main>

        <footer className="app-footer">
          <span>© {new Date().getFullYear()} LeaveFlow</span>

          <span>
            Votre équipe. Votre temps. Votre équilibre.
          </span>
        </footer>
      </div>
    </div>
  );
}
