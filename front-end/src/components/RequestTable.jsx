import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useApp } from '../context/AppContext';
import { send } from '../services/api';
import { Avatar, Badge, Empty, date, Modal } from './UI';
export default function RequestTable({ rows = [], decision = false }) {
  const { db, user, run } = useApp(); const [selected, setSelected] = useState(null);
  const [comment, setComment] = useState(''); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  async function act(status) {
    if(status === 'REJETÉE' && !comment.trim()) { setError('Veuillez préciser le motif du rejet.'); return; }
    setError(''); setBusy(true);
    const result = await run(() => send(`/demandes/${selected.id}/${status === 'APPROUVÉE' ? 'approuver' : 'refuser'}`, status === 'APPROUVÉE' ? {} : { motifRejet: comment }), 'Décision enregistrée dans PostgreSQL.');
    setBusy(false); if(result) {setSelected(null);setComment('');}
  }
  const managerTeam = db.teams.find(t => t.managerId === user.id)?.name || user.team;
  const canApprove = (r, owner) => r.status === 'SOUMISE' && r.userId !== user.id && (user.role === 'rh' || (user.role === 'manager' && owner?.role === 'employe' && owner.team === managerTeam));
  return <>{!rows.length ? <Empty/> : <div className="table-scroll"><table><thead><tr><th>Collaborateur</th><th>Type de congé</th><th>Période</th><th>Durée</th><th>Statut</th><th/></tr></thead><tbody>
    {rows.map(r => {const owner = db.users.find(u => u.id === r.userId); return <tr key={r.id}>
      <td><div className="person"><Avatar name={owner?.name || r.userName || 'Utilisateur'}/><div><strong>{owner?.name || r.userName}</strong><small>{owner?.team || r.userTeam}</small></div></div></td>
      <td>{r.type}</td><td>{date(r.start)} → {date(r.end)}</td><td><b>{r.days}</b> j</td><td><Badge status={r.status}/></td>
      <td>{decision && canApprove(r,owner) ? <button className="text-btn" onClick={() => {setSelected(r);setError('');setComment('');}}>Examiner ↗</button> : <Link className="text-btn" to={`/${user.role}/demandes/${r.id}`}>Détails ↗</Link>}</td>
    </tr>;})}</tbody></table></div>}
    {selected && <Modal title={`Examiner la demande #${selected.id}`} onClose={() => setSelected(null)}>
      <p>{selected.type} · {selected.days} jours · {date(selected.start)} au {date(selected.end)}</p><div className="note">{selected.reason || 'Aucun motif renseigné'}</div>
      <label>Motif du refus / commentaire</label><textarea value={comment} onChange={e => setComment(e.target.value)} placeholder="Précisez votre décision…" />
      {error && <p className="error" role="alert">{error}</p>}
      <div className="button-group"><button disabled={busy} className="btn danger" onClick={() => act('REJETÉE')}>Refuser</button><button disabled={busy} className="btn" onClick={() => act('APPROUVÉE')}>Approuver</button></div>
    </Modal>}
  </>;
}
