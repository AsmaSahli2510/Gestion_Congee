import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import AuthLayout from '../components/AuthLayout';
import { useApp } from '../context/AppContext';
import { registerCompany, loginRequest } from '../services/authService';
export default function CreateCompany() {
  const { login } = useApp(); const nav = useNavigate();
  const [error, setError] = useState(''); const [loading, setLoading] = useState(false);
  async function submit(e) {
    e.preventDefault(); setError(''); setLoading(true);
    const f = new FormData(e.target);
    const name = String(f.get('name')).trim().split(/\s+/);
    if (String(f.get('password')).length < 6) { setError('Mot de passe : 6 caractères minimum.'); setLoading(false); return; }
    try {
      await registerCompany({ nomEntreprise: f.get('company'), prenomRh: name.shift(), nomRh: name.join(' ') || '-', emailRh: f.get('email'), motDePasse: f.get('password') });
      await loginRequest(f.get('email'), f.get('password'));
      await login();
      nav('/rh/dashboard', { replace: true });
    } catch (err) { setError(err.message); } finally { setLoading(false); }
  }
  return <AuthLayout><Link className="back" to="/signup">← Choisir un autre parcours</Link>
    <h2>Un nouvel espace commence.</h2><p className="sub">Créez votre entreprise et devenez son administrateur RH.</p>
    <form onSubmit={submit}>
      <label className="field" htmlFor="company">Nom de l’entreprise</label><div className="input-box"><input style={{paddingLeft:16}} id="company" name="company" required /></div>
      <label className="field" htmlFor="name">Votre nom complet</label><div className="input-box"><input style={{paddingLeft:16}} id="name" name="name" required /></div>
      <label className="field" htmlFor="email">Email professionnel</label><div className="input-box"><input style={{paddingLeft:16}} id="email" name="email" type="email" required /></div>
      <label className="field" htmlFor="password">Mot de passe (6 caractères minimum)</label><div className="input-box"><input style={{paddingLeft:16}} id="password" name="password" type="password" autoComplete="new-password" minLength={6} required /></div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <button className="submit" disabled={loading}>{loading?'Création…':'Créer mon entreprise →'}</button>
    </form>
  </AuthLayout>;
}
