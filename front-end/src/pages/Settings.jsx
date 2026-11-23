import React from 'react';
import { useApp } from '../context/AppContext';
import { send } from '../services/api';
import { Heading } from '../components/UI';
export default function Settings() {
  const {db,run}=useApp();const company=db.company;
  async function save(e) {e.preventDefault();const f=new FormData(e.target);
    await run(()=>send('/entreprise',{nom:company.name,joursCongesAnnuels:Number(f.get('days'))},'PUT'),'Politique annuelle mise à jour.');
  }
  return <><Heading title="Les bonnes règles, dès le départ." sub="Paramètres réellement disponibles dans le backend."/>
    <div className="two-columns"><section className="panel"><h2>Politique de congés</h2><form onSubmit={save} key={company.id||'policy'}>
      <label>Droits annuels (jours)</label><input name="days" type="number" min="0" max="365" step="1" defaultValue={company.joursCongesAnnuels??30} required/>
      <p className="muted">Cette valeur sert à l’attribution initiale des congés. Elle ne crédite pas rétroactivement les soldes existants.</p>
      <button className="btn">Enregistrer</button>
    </form></section>
    <section className="panel"><h2>Types de congés</h2><p className="muted">Ces types sont définis dans le modèle Java du backend et ne sont pas modifiables dans cette version.</p>
      {db.types.map(t=><div key={t.id} className="setting-row"><span>{t.name}</span><span className="muted">{t.deduct?'Déduit du solde':'Sans déduction'}</span></div>)}
    </section></div>
  </>;
}
