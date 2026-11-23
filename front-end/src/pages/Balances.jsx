import React,{useState} from 'react';
import { Wallet } from 'lucide-react';
import { useApp } from '../context/AppContext';
import useScope from '../context/useScope';
import { send } from '../services/api';
import { Heading,Avatar,Modal,Export,Empty } from '../components/UI';
export default function Balances() {
  const {db,user,run}=useApp();const {members}=useScope();const [edit,setEdit]=useState(null);const [busy,setBusy]=useState(false);
  const ids=members.map(u=>u.id),movements=db.movements.filter(m=>ids.includes(m.userId));
  async function save(e){e.preventDefault();const f=new FormData(e.target);const variation=Number(f.get('amount'));
    if(!Number.isInteger(variation))return;
    setBusy(true);const result=await run(()=>send('/soldes/ajuster',{userId:edit.id,variation,raison:f.get('reason')}),'Ajustement enregistré.');setBusy(false);if(result)setEdit(null);
  }
  return <><Heading title={user.role==='employe'?'Du temps pour vous.':'Les soldes, en toute clarté.'} sub="Chaque jour compte. Chaque mouvement reste traçable."><Export rows={members.map(u=>({nom:u.name,equipe:u.team,solde:u.balance}))}/></Heading>
    <div className="balance-banner"><div><span className="eyebrow">VOTRE SOLDE PERSONNEL</span><h2>{user.balance} <small>jours disponibles</small></h2></div><Wallet size={60}/><span>Congés annuels : {db.company.joursCongesAnnuels ?? 30} j</span></div>
    <section className="panel"><h2>{user.role==='employe'?'Mon récapitulatif':'Soldes des collaborateurs'}</h2>
      <div className="table-scroll"><table><thead><tr><th>Collaborateur</th><th>Équipe</th><th>Disponible</th><th/></tr></thead><tbody>{members.map(u=><tr key={u.id}>
        <td><div className="person"><Avatar name={u.name}/><strong>{u.name}</strong></div></td><td>{u.team}</td><td><b className="balance-value">{u.balance}</b> jours</td><td>{user.role==='rh'&&<button className="text-btn" onClick={()=>setEdit(u)}>Ajuster ↗</button>}</td>
      </tr>)}</tbody></table></div>
    </section><section className="panel"><h2>Journal des mouvements accessibles</h2>{movements.length?<div className="table-scroll"><table><thead><tr><th>Date</th><th>Collaborateur</th><th>Variation</th><th>Motif</th><th>Auteur</th></tr></thead><tbody>{movements.map(m=><tr key={m.id}><td>{m.date}</td><td>{db.users.find(u=>u.id===m.userId)?.name || m.userName}</td><td className={m.amount>0?'positive':'negative'}>{m.amount>0?'+':''}{m.amount} j</td><td>{m.raison||m.reason}</td><td>{m.actor}</td></tr>)}</tbody></table></div>:<Empty text="Aucun mouvement disponible."/>}</section>
    {edit&&<Modal title={`Ajuster le solde de ${edit.name}`} onClose={()=>setEdit(null)}><form onSubmit={save}><label>Variation (+ crédit / − débit)</label><input name="amount" type="number" step="1" required/><label>Motif obligatoire</label><textarea name="reason" required/><button className="btn" disabled={busy}>Valider l’ajustement</button></form></Modal>}
  </>;
}
