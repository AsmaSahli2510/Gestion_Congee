import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { CalendarDays } from 'lucide-react';
import { useApp, workdays } from '../context/AppContext';
import { send } from '../services/api';
import { Heading } from '../components/UI';
export default function NewRequest() {
  const { db, user, run } = useApp(); const { id } = useParams(); const nav = useNavigate();
  const existing = db.mine.find(r => r.id === Number(id));
  const [start, setStart] = useState(''); const [end, setEnd] = useState(''); const [type, setType] = useState('Congés payés'); const [error, setError] = useState(''); const [busy, setBusy] = useState(false);
  useEffect(() => { if(existing) { setStart(existing.start); setEnd(existing.end); setType(existing.type); } }, [existing?.id]);
  const days = workdays(start, end); const deduct = db.types.find(t => t.name === type)?.deduct;
  async function submit(e) {
    e.preventDefault(); setError('');
    if (id && !existing) return setError('Demande introuvable ou inaccessible.');
    if (existing && existing.status !== 'SOUMISE') return setError('Seules les demandes en attente sont modifiables.');
    if (days < 1) return setError('Choisissez une période valide avec au moins un jour ouvré.');
    if (deduct && days > user.balance) return setError('Solde insuffisant.');
    const f = new FormData(e.target); const file = f.get('attachment');
    const payload = { dateDebut: start, dateFin: end, typeConge: type, motif: f.get('reason') || '', justificatif: file?.name || existing?.justificatif || '' };
    setBusy(true);
    const result = await run(() => send(existing ? `/demandes/${existing.id}` : '/demandes', payload, existing ? 'PUT' : 'POST'), 'Demande enregistrée dans PostgreSQL.');
    setBusy(false); if(result) nav(`/${user.role}/demandes`);
  }
  return <><Heading title={existing?'Modifier ma demande':'Votre prochaine pause commence ici.'} sub="Choisissez vos dates. Votre responsable examinera la demande."/>
    <div className="form-grid"><section className="panel"><form onSubmit={submit}>
      <div className="section-head"><h2>Détails de la demande</h2><CalendarDays color="#006ee9"/></div>
      <label>Type de congé</label><select value={type} onChange={e => setType(e.target.value)} required>{db.types.map(t => <option key={t.id}>{t.name}</option>)}</select>
      <div className="two-fields"><div><label>Date de début</label><input type="date" value={start} onChange={e => setStart(e.target.value)} required /></div><div><label>Date de fin</label><input type="date" value={end} min={start} onChange={e => setEnd(e.target.value)} required /></div></div>
      <label>Motif</label><textarea name="reason" defaultValue={existing?.reason || ''} key={existing?.id || 'new'} placeholder="Quelques mots pour accompagner votre demande…" />
      <label>Référence justificatif (nom seulement)</label><input name="attachment" type="file" accept=".pdf,.png,.jpg,.jpeg"/>
      <p className="muted">Le backend actuel ne gère pas le stockage de fichiers : seul le nom du justificatif est enregistré.</p>
      {error && <p className="error" role="alert">{error}</p>}
      <div className="form-actions"><button className="btn secondary" type="button" onClick={() => nav(-1)}>Retour</button><button disabled={busy} className="btn">{busy?'Enregistrement…':'Soumettre ma demande →'}</button></div>
    </form></section><aside className="summary-card"><span className="eyebrow">VOTRE DEMANDE EN UN REGARD</span>
      <div className="summary-days">{days}<small>jours ouvrés</small></div><div className="summary-row"><span>Solde actuel</span><b>{user.balance} j</b></div>
      <div className="summary-row"><span>Solde après approbation</span><b>{user.balance - (deduct ? days : 0)} j</b></div>
      <p>Les samedis et dimanches sont exclus. Le calcul définitif est contrôlé par le backend.</p><div className="note">Le solde n’est débité qu’après validation.</div>
    </aside></div>
  </>;
}
