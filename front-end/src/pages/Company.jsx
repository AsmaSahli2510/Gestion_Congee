import React from 'react';
import { useApp } from '../context/AppContext';
import { send } from '../services/api';
import { Heading } from '../components/UI';
import logo from '../images/logo.png';
export default function Company() {
  const { db, run } = useApp();
  async function save(e) { e.preventDefault();const f=new FormData(e.target);
    await run(()=>send('/entreprise',{nom:f.get('name'),secteur:f.get('sector'),description:f.get('description')},'PUT'),'Entreprise mise à jour.');
  }
  return <><Heading title="Votre entreprise, votre espace." sub="Une identité commune pour vos équipes."/>
    <div className="form-grid"><section className="panel"><form onSubmit={save} key={db.company.id||'company'}><h2>Informations de l’entreprise</h2>
      <label>Nom</label><input name="name" defaultValue={db.company.name||''} required/>
      <label>Secteur d’activité</label><input name="sector" defaultValue={db.company.secteur||''}/>
      <label>Description</label><textarea name="description" defaultValue={db.company.description||''}/><button className="btn">Enregistrer</button>
    </form></section><aside className="summary-card"><span className="eyebrow">VOTRE PLATEFORME</span>
      <img style={{width:'100%',margin:'25px 0'}} src={logo} alt="LeaveFlow"/><p>Un espace dédié à votre organisation, à ses équipes et à leurs temps de repos.</p>
      <div className="note">Les accès sont limités côté serveur à votre entreprise et à votre rôle.</div>
    </aside></div>
  </>;
}
