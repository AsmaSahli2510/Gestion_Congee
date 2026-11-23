import React,{useState} from 'react';
import { Plus } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { api,send } from '../services/api';
import { Heading,Modal,Empty } from '../components/UI';
export default function Invitations() {
  const {db,run,notify}=useApp();const [open,setOpen]=useState(false);const [latest,setLatest]=useState('');
  const invitationLink=(token)=>`${window.location.origin}/invitation?token=${encodeURIComponent(token)}`;
  async function copy(link) {try{await navigator.clipboard.writeText(link);notify('Lien d’activation copié.');}catch{notify('Copie impossible. Sélectionnez le lien affiché.');}}
  async function create(e){e.preventDefault();const f=new FormData(e.target);
    const result=await run(()=>send('/invitations',{email:f.get('email'),role:String(f.get('role')).toUpperCase(),equipeId:f.get('team')?Number(f.get('team')):null,poste:f.get('poste')||''}),'Invitation créée dans PostgreSQL.');
    if(result){setOpen(false);setLatest(invitationLink(result.token||result.invitation?.token));}
  }
  return <><Heading title="Le prochain talent commence ici." sub="Invitez vos collaborateurs dans votre espace privé."><button className="btn" onClick={()=>setOpen(true)}><Plus size={16}/>Créer une invitation</button></Heading>
    <div className="note">Chaque invitation expire après 7 jours. Le serveur crée un lien sécurisé : copiez-le et envoyez-le à votre collaborateur. Aucun email automatique n’est configuré.</div>
    {latest&&<div className="note"><strong>Dernière invitation :</strong> <input readOnly value={latest} onFocus={e=>e.target.select()}/><button className="text-btn" onClick={()=>copy(latest)}>Copier le lien</button></div>}
    <section className="panel"><div className="table-scroll"><table><thead><tr><th>Email</th><th>Rôle</th><th>Équipe</th><th>Code</th><th>Statut</th><th/></tr></thead><tbody>{db.invitations.map(i=><tr key={i.id}>
      <td>{i.email}</td><td>{String(i.role).toLowerCase()}</td><td>{i.team||'Non affecté'}</td><td><code>{String(i.code).slice(0,8)}…</code></td>
      <td>{i.used?'Utilisée':Date.now()>i.expires?'Expirée':'En attente'}</td><td><button className="text-btn" onClick={()=>copy(invitationLink(i.token))}>Copier le lien</button> {!i.used&&<button className="text-btn danger-text" onClick={()=>{if(window.confirm('Révoquer cette invitation ?'))run(()=>api(`/invitations/${i.id}`,{method:'DELETE'}),'Invitation révoquée.');}}>Révoquer</button>}</td>
    </tr>)}</tbody></table></div>{!db.invitations.length&&<Empty text="Aucune invitation pour le moment."/>}</section>
    {open&&<Modal title="Inviter un collaborateur" onClose={()=>setOpen(false)}><form onSubmit={create}>
      <label>Email professionnel</label><input name="email" type="email" required/>
      <label>Rôle</label><select name="role"><option value="employe">Employé</option><option value="manager">Manager</option><option value="rh">RH</option></select>
      <label>Équipe</label><select name="team"><option value="">Non affecté</option>{db.teams.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}</select>
      <label>Poste</label><input name="poste" placeholder="Facultatif"/><button className="btn">Créer l’invitation</button>
    </form></Modal>}
  </>;
}
