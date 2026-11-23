import React,{useState} from 'react';
import { Plus, Users } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { api, send } from '../services/api';
import { Heading, Avatar, Modal, Empty } from '../components/UI';
export default function Teams() {
  const {db,run} = useApp(); const [edit,setEdit] = useState(null); const [busy,setBusy]=useState(false);
  async function save(e) {
    e.preventDefault();const f=new FormData(e.target);setBusy(true);
    const payload={nom:String(f.get('name')).trim(),description:String(f.get('description')||''),managerId:f.get('manager')?Number(f.get('manager')):null,active:true};
    const result=await run(() => send(edit.id?`/equipes/${edit.id}`:'/equipes',payload,edit.id?'PUT':'POST'),'Équipe enregistrée.');
    setBusy(false);if(result)setEdit(null);
  }
  return <><Heading title="Des équipes qui avancent ensemble." sub="Structurez votre organisation et désignez les bons relais."><button className="btn" onClick={()=>setEdit({})}><Plus size={16}/>Créer une équipe</button></Heading>
    <div className="team-grid">{db.teams.map(team => {const members=db.users.filter(u=>u.team===team.name);const manager=db.users.find(u=>u.id===team.managerId);return <section className="team-card" key={team.id}>
      <div className="section-head"><span className="team-icon"><Users/></span><span className="pill">{members.length} membres</span></div>
      <h2>{team.name}</h2><p className="muted">Responsable · {manager?.name||team.managerName||'Non affecté'}</p>
      <div className="team-avatars">{members.map(u=><Avatar key={u.id} name={u.name}/>)}</div>
      <div className="section-head"><button className="text-btn" onClick={()=>setEdit(team)}>Gérer l’équipe ↗</button><button className="text-btn danger-text" onClick={()=>{if(window.confirm('Supprimer cette équipe ?'))run(()=>api(`/equipes/${team.id}`,{method:'DELETE'}),'Équipe supprimée.');}}>Supprimer</button></div>
    </section>;})}</div>
    {!db.teams.length&&<Empty text="Créez votre première équipe."/>}
    {edit&&<Modal title={edit.id?'Modifier l’équipe':'Nouvelle équipe'} onClose={()=>setEdit(null)}><form onSubmit={save}>
      <label>Nom de l’équipe</label><input name="name" defaultValue={edit.name||''} required/>
      <label>Description</label><textarea name="description" defaultValue={edit.description||''}/>
      <label>Manager</label><select name="manager" defaultValue={edit.managerId||''}><option value="">Non affecté</option>{db.users.filter(u=>u.role==='manager'||u.role==='rh').map(u=><option key={u.id} value={u.id}>{u.name}</option>)}</select>
      <button className="btn" disabled={busy}>{busy?'Enregistrement…':'Enregistrer'}</button>
    </form></Modal>}
  </>;
}
