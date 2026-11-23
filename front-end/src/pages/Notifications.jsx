import React from 'react';
import { Bell,CheckCheck } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { api } from '../services/api';
import { Heading,Empty } from '../components/UI';
export default function Notifications() {
  const {db,user,run}=useApp();const rows=db.notifications.filter(n=>n.userId===user.id);
  return <><Heading title="Vous êtes dans la boucle." sub="Les informations utiles, au même endroit."><button className="btn secondary" onClick={()=>run(()=>api('/notifications/toutes-lues',{method:'POST'}),'Toutes les notifications sont lues.')}><CheckCheck size={17}/>Tout marquer comme lu</button></Heading>
    <section className="panel">{rows.map(n=><button className={'notification '+(!n.read?'unread':'')} key={n.id} onClick={()=>!n.read&&run(()=>api(`/notifications/${n.id}/lire`,{method:'POST'}),null)}>
      <span className="stat-icon blue"><Bell size={19}/></span><div><strong>{n.text||n.message}</strong><small>{n.read?'Lu':'Cliquez pour marquer comme lu'}</small></div>{!n.read&&<i/>}
    </button>)}{!rows.length&&<Empty text="Tout est calme. Vos prochaines notifications apparaîtront ici."/>}</section>
  </>;
}
