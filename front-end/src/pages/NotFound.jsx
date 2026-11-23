import React from 'react';
import { Link } from 'react-router-dom';
export default function NotFound(){return <main className="auth"><div className="form-side" style={{margin:'auto',textAlign:'center',padding:48}}><h1>Page introuvable</h1><p>Cette page n’existe pas.</p><Link className="btn" to="/login">Retour à la connexion</Link></div></main>}
