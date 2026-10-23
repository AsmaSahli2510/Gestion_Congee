
import React from "react";
import { Link } from "react-router-dom";

import {
  ArrowUpRight,
  Plus,
  CalendarDays,
  Users,
  Clock,
  Wallet,
} from "lucide-react";

import { useApp } from "../context/AppContext";
import useScope from "../context/useScope";
import { Heading, Avatar } from "../components/UI";
import RequestTable from "../components/RequestTable";

export default function Dashboard() {
  const { user } = useApp();
  const { members, requests, mine, pending } = useScope();

  if (!user) {
    return <p>Chargement de votre espace...</p>;
  }

  const role = String(user.role || "").trim().toLowerCase();
  const root = "/" + role;
  const employee = role === "employe";

  const userName =
    user.prenom || user.nom
      ? `${user.prenom || ""} ${user.nom || ""}`.trim()
      : user.name || user.email || "Utilisateur";

  const firstName = userName.split(" ")[0];

  const balance = Number(user.balance ?? 0);

  const approved = requests.filter(
    (request) =>
      ["APPROUVÉE", "APPROUVEE", "APPROVED"].includes(
        String(request.status || "").toUpperCase()
      )
  ).length;

  const waiting = employee
    ? mine.filter((request) =>
        ["SOUMISE", "EN_ATTENTE", "PENDING"].includes(
          String(request.status || "").toUpperCase()
        )
      ).length
    : pending.length;

  const teamsCount = new Set(
    members.map((member) => member.team).filter(Boolean)
  ).size;

  const stats = [
    [
      employee ? "Mes demandes" : "Collaborateurs",
      employee ? mine.length : members.length,
      Users,
      "blue",
    ],
    ["En attente", waiting, Clock, "violet"],
    ["Congés approuvés", approved, CalendarDays, "lime"],
    ["Équipes représentées", teamsCount, Users, "cyan"],
  ];

  return (
    <>
      <Heading
        title={`Bonjour, ${firstName} ☀`}
        sub={
          employee
            ? "Une pause se prépare ? Retrouvez tous vos congés ici."
            : "Une vue claire pour une équipe bien organisée."
        }
      >
        <Link className="btn" to={`${root}/nouvelle-demande`}>
          <Plus size={17} />
          Nouvelle demande
        </Link>
      </Heading>

      <div className="dashboard-grid">
        {/* Carte principale */}
        <section className="hero-card">
          <div>
            <span className="eyebrow">
              PRENDRE DU TEMPS, ÇA SE PRÉPARE
            </span>

            <h2>
              Une équipe alignée.
              <br />
              Des pauses bien méritées.
            </h2>

            <p>
              {employee
                ? "Votre prochain moment pour vous commence ici."
                : "Anticipez les absences et gardez le rythme, ensemble."}
            </p>

            <Link to={`${root}/calendrier`}>
              Explorer le calendrier <ArrowUpRight size={17} />
            </Link>
          </div>

          <div className="hero-art">
            <div className="mini-calendar">
              <span>
                {new Intl.DateTimeFormat("fr-FR", {
                  month: "long",
                })
                  .format(new Date())
                  .toUpperCase()}
              </span>

              <strong>
                {new Date().getDate()}
              </strong>

              <small>Votre prochaine pause</small>

              <div>● ● ● ● ●</div>
            </div>

            <span className="floating-tick">✓</span>
            <span className="petal" />
          </div>
        </section>

        {/* Solde de congés */}
        <section className="balance-card">
          <div className="section-head">
            <span>Mon solde de congés</span>
            <Wallet size={19} />
          </div>

          <div className="balance-number">
            {balance}
            <span>jours disponibles</span>
          </div>

          <div className="balance-track">
            <i
              style={{
                width: `${Math.max(
                  0,
                  Math.min(100, (balance / 30) * 100)
                )}%`,
              }}
            />
          </div>

          <Link to={`${root}/soldes`}>
            Voir le détail <ArrowUpRight size={16} />
          </Link>
        </section>
      </div>

      {/* Statistiques */}
      <div className="stats-grid">
        {stats.map(([label, value, Icon, color]) => (
          <section className="stat" key={label}>
            <span className={`stat-icon ${color}`}>
              <Icon size={21} />
            </span>

            <div>
              <p>{label}</p>
              <strong>{value}</strong>
            </div>

            <ArrowUpRight size={15} />
          </section>
        ))}
      </div>

      {/* Demandes récentes */}
      <section className="panel">
        <div className="section-head">
          <div>
            <h2>
              {employee
                ? "Mes dernières demandes"
                : "Les demandes à suivre"}
            </h2>

            <p>Le bon aperçu, au bon moment.</p>
          </div>

          <Link
            className="text-btn"
            to={`${root}${
              employee ? "/demandes" : "/validations"
            }`}
          >
            Tout voir ↗
          </Link>
        </div>

        <RequestTable
          rows={(employee ? mine : pending).slice(0, 5)}
          decision={!employee}
        />
      </section>

      {/* Équipe et conseil */}
      <div className="two-columns">
        <section className="panel">
          <div className="section-head">
            <h2>L’équipe en un coup d’œil</h2>

            <span className="pill">
              {members.length} personnes
            </span>
          </div>

          <div className="member-preview">
            {members.slice(0, 4).map((member) => {
              const memberName =
                member.name ||
                `${member.prenom || ""} ${member.nom || ""}`.trim() ||
                member.email ||
                "Utilisateur";

              return (
                <div key={member.id}>
                  <Avatar name={memberName} />

                  <strong>{memberName}</strong>

                  <small>
                    {member.team || "Équipe non renseignée"}
                  </small>

                  <span className="available">
                    ●{" "}
                    {member.active === false
                      ? "Désactivé"
                      : "Compte actif"}
                  </span>
                </div>
              );
            })}

            {members.length === 0 && (
              <p>
                Les informations de votre équipe apparaîtront
                ici lorsque les données seront connectées.
              </p>
            )}
          </div>
        </section>

        <section className="tip-card">
          <span className="eyebrow">LE PETIT RAPPEL</span>

          <h2>
            Une absence anticipée,
            <br />
            une équipe sereine.
          </h2>

          <p>
            Pensez à consulter le calendrier de votre équipe
            avant de choisir vos dates.
          </p>

          <Link to={`${root}/calendrier`}>
            Préparer ma prochaine pause ↗
          </Link>
        </section>
      </div>
    </>
  );
}
