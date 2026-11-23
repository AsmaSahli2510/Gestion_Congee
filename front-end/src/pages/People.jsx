import React,{useState} from 'react';
import { Link } from 'react-router-dom';
import { Plus,Search } from 'lucide-react';
import { useApp } from '../context/AppContext';
import useScope from '../context/useScope';
import { api,send } from '../services/api';
import { Heading,Avatar,Modal,Export,Empty } from '../components/UI';
export default function People() {
  const {db,user,run}=useApp();const {members}=useScope();const [q,setQ]=useState('');const [edit,setEdit]=useState(null);
  const rows=members.filter(u=>`${u.name} ${u.email} ${u.team}`.toLowerCase().includes(q.toLowerCase()));
  async function save(e) {
    e.preventDefault();const f=new FormData(e.target);const team=db.teams.find(t=>String(t.id)===f.get('team'));
    let ok=true;
    if((team?.id??null)!==(edit.equipeId??null)) ok=!!(await run(()=>send(`/utilisateurs/${edit.id}/affecter`,{equipeId:team?.id??null}),'Affectation enregistrée.'));
    if(ok&&edit.id!==user.id&&(f.get('active')==='on')!==edit.active) ok=!!(await run(()=>api(`/utilisateurs/${edit.id}/statut`,{method:'POST'}),'Statut mis à jour.'));
    if(ok)setEdit(null);
  }
  return <><Heading title={user.role==='rh'?'Les talents de votre entreprise.':'Votre équipe, simplement.'} sub="Des personnes, des rôles, une organisation claire.">{user.role==='rh'&&<Link className="btn" to="/rh/invitations"><Plus size={16}/>Inviter un collaborateur</Link>}</Heading>
    <section className="panel"><div className="filters"><div className="search"><Search size={17}/><input value={q} onChange={e=>setQ(e.target.value)} placeholder="Nom, email ou équipe…"/></div><Export rows={rows.map(({name,email,role,team,active})=>({name,email,role,team,active}))}/></div>
      <div className="table-scroll"><table><thead><tr><th>Collaborateur</th><th>Email</th><th>Équipe</th><th>Rôle</th><th>Statut</th><th/></tr></thead><tbody>{rows.map(u=><tr key={u.id}>
        <td><div className="person"><Avatar name={u.name}/><strong>{u.name}</strong></div></td><td>{u.email}</td><td>{u.team||'Non affecté'}</td><td>{u.role}</td>
        <td><span className={'pill '+(!u.active?'inactive':'')}>{u.active?'Actif':'Désactivé'}</span></td><td>{user.role==='rh'&&<button className="text-btn" onClick={()=>setEdit(u)}>Gérer ↗</button>}</td>
      </tr>)}</tbody></table></div>{!rows.length&&<Empty/>}</section>
    {edit&&<Modal title={`Gérer ${edit.name}`} onClose={()=>setEdit(null)}><form onSubmit={save}>
      <label>Équipe</label><select name="team" defaultValue={edit.equipeId||''}><option value="">Non affecté</option>{db.teams.map(t=><option key={t.id} value={t.id}>{t.name}</option>)}</select>
      <label>Rôle (défini lors de l’invitation)</label><input readOnly value={edit.role}/>
      <label className="checkbox-label"><input type="checkbox" name="active" defaultChecked={edit.active} disabled={edit.id===user.id}/>Compte actif</label>
      <button className="btn">Enregistrer</button>
    </form></Modal>}
  </>;
}
