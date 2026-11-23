import React,{useState} from 'react';
import { useApp } from '../context/AppContext';
import { send } from '../services/api';
import { Heading,Avatar } from '../components/UI';
export default function Profile() {
  const {user,run}=useApp();const [error,setError]=useState('');
  async function save(e){e.preventDefault();setError('');const f=new FormData(e.target);
    const password=f.get('newPassword');
    if(password && !f.get('oldPassword'))return setError('L’ancien mot de passe est requis.');
    if(password && password.length<6)return setError('Le nouveau mot de passe doit comporter 6 caractères minimum.');
    const result=await run(()=>send('/utilisateurs/profil',{nom:f.get('lastName'),prenom:f.get('firstName'),telephone:f.get('phone'),ancienMotDePasse:f.get('oldPassword'),nouveauMotDePasse:password},'PUT'),'Profil mis à jour.');
    if(result){e.target.elements.oldPassword.value='';e.target.elements.newPassword.value='';}
  }
  return <><Heading title="Un espace à votre image." sub="Mettez à jour vos informations personnelles."/>
    <div className="form-grid"><section className="panel"><form onSubmit={save} key={user.id}><div className="profile-head"><Avatar name={user.name}/><div><h2>{user.name}</h2><span className="pill">{user.role}</span></div></div>
      <label>Prénom</label><input name="firstName" defaultValue={user.prenom||''} required/>
      <label>Nom</label><input name="lastName" defaultValue={user.nom||''} required/>
      <label>Téléphone</label><input name="phone" defaultValue={user.telephone||''}/>
      <label>Email professionnel</label><input value={user.email} readOnly/>
      <label>Équipe</label><input value={user.team||'Non affecté'} readOnly/>
      <h2>Changer mon mot de passe</h2><label>Ancien mot de passe</label><input type="password" name="oldPassword" autoComplete="current-password"/>
      <label>Nouveau mot de passe</label><input type="password" name="newPassword" minLength={6} autoComplete="new-password"/>
      {error&&<p className="error" role="alert">{error}</p>}
      <button className="btn">Enregistrer les modifications</button>
    </form></section><aside className="summary-card"><h2>Vos accès</h2><p>Votre rôle et votre équipe sont gérés par votre responsable RH.</p><div className="note">Les mots de passe sont hachés avec BCrypt côté Spring Security. Aucune donnée sensible n’est sauvegardée dans le navigateur.</div></aside></div>
  </>;
}
