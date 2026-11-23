import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import AuthLayout from '../components/AuthLayout';
import { activateInvitation, checkInvitation } from '../services/authService';
export default function Invitation() {
  const [params] = useSearchParams(); const nav = useNavigate();
  const [token, setToken] = useState(params.get('token') || params.get('code') || '');
  const [info, setInfo] = useState(null); const [error, setError] = useState(''); const [loading, setLoading] = useState(false);
  useEffect(() => { if (!token.trim()) { setInfo(null); return; } let live = true;
    checkInvitation(token.trim()).then(i => { if(live){setInfo(i);setError('');} }).catch(e => { if(live){setInfo(null);setError(e.message);} });
    return () => { live = false; };
  }, [token]);
  async function submit(e) { e.preventDefault(); setLoading(true); setError(''); const f = new FormData(e.target);
    const parts = String(f.get('name')).trim().split(/\s+/);
    try {
      await activateInvitation({ token: token.trim(), prenom: parts.shift(), nom: parts.join(' ') || '-', motDePasse: f.get('password') });
      nav('/login', { replace: true, state: { message: 'Compte activé. Connectez-vous.' } });
    } catch (err) { setError(err.message); } finally { setLoading(false); }
  }
  return <AuthLayout><Link to="/signup" className="back">← Retour</Link><h2>Votre équipe vous attend.</h2>
    <p className="sub">Votre rôle et votre entreprise sont définis par votre RH.</p>
    <label className="field" htmlFor="token">Code d’invitation</label><div className="input-box"><input style={{paddingLeft:16}} id="token" value={token} onChange={e => setToken(e.target.value)} placeholder="Code reçu de votre RH" /></div>
    {info && <p className="note">{info.entrepriseNom} · {info.role} · {info.email}</p>}
    <form onSubmit={submit}>
      <label className="field" htmlFor="name">Nom complet</label><div className="input-box"><input style={{paddingLeft:16}} name="name" id="name" required /></div>
      <label className="field" htmlFor="password">Mot de passe</label><div className="input-box"><input style={{paddingLeft:16}} name="password" id="password" type="password" minLength={6} autoComplete="new-password" required /></div>
      {error && <p role="alert" className="form-error">{error}</p>}
      <button className="submit" disabled={!info || loading}>{loading?'Activation…':'Activer mon compte →'}</button>
    </form>
  </AuthLayout>;
}
