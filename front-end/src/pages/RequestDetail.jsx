import React from 'react';
import { Link, useParams } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { api } from '../services/api';
import { Heading, Badge, date, Empty } from '../components/UI';
export default function RequestDetail() {
  const { id } = useParams(); const { db, user, run } = useApp();
  const r = db.requests.find(item => item.id === Number(id));
  if (!r) return <Empty text="Demande introuvable dans votre espace." />;
  const author = db.users.find(u => u.id === r.userId);
  return <><Heading title={`Demande #${r.id}`} sub={`${author?.name || r.userName || 'Collaborateur'} · ${author?.team || r.userTeam || ''}`}><Badge status={r.status}/></Heading>
    <div className="form-grid"><section className="panel"><h2>{r.type}</h2><div className="detail-grid">
      {[["Début",date(r.start)],["Fin",date(r.end)],["Durée",`${r.days} jours ouvrés`],["Déposée le",date(r.created)],["Motif",r.reason || 'Non précisé'],["Justificatif",r.justificatif || 'Aucun']].map(([label,value]) => <div key={label}><small>{label}</small><strong>{value}</strong></div>)}
    </div>{r.comment && <div className="note">Décision de {r.actor || 'votre responsable'} : {r.comment}</div>}
    {r.userId === user.id && r.status === 'SOUMISE' && <div className="button-group">
      <Link className="btn" to={`/${user.role}/demandes/${r.id}/modifier`}>Modifier</Link>
      <button className="btn danger" onClick={() => { if(window.confirm('Annuler cette demande ?')) run(() => api(`/demandes/${r.id}`, {method:'DELETE'}), 'Demande annulée.'); }}>Annuler la demande</button>
    </div>}</section><section className="summary-card"><h2>Suivi de la demande</h2><div className="timeline"><p>● Demande soumise <small>{date(r.created)}</small></p><p>● {r.status === 'SOUMISE' ? 'En cours d’examen' : r.status}</p><p>● {r.status === 'APPROUVÉE' ? 'Solde mis à jour' : 'Notification après décision'}</p></div></section></div>
  </>;
}
