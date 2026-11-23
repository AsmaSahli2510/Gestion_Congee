import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { getCurrentUser, logoutRequest } from '../services/authService';
import { api } from '../services/api';

const Context = createContext(null);
// Valeurs fixes du backend (TypeConge) : pas de création fictive de types côté React.
export const LEAVE_TYPES = [
  { id: 'ANNUEL', name: 'Congés payés', deduct: true },
  { id: 'RTT', name: 'RTT', deduct: true },
  { id: 'MALADIE', name: 'Maladie', deduct: false },
  { id: 'EXCEPTIONNEL', name: 'Événement Familial / Exceptionnel', deduct: false },
  { id: 'SANS_SOLDE', name: 'Sans solde', deduct: false },
  { id: 'MATERNITE', name: 'Congé Maternité', deduct: false },
  { id: 'PATERNITE', name: 'Congé Paternité', deduct: false },
];
const emptyDB = { users: [], requests: [], mine: [], pending: [], teams: [], invitations: [], movements: [], notifications: [], audit: [], types: LEAVE_TYPES, company: { name: '', acquisition: 2.5 } };
const normalizeUser = (u, extra = {}) => ({ ...u, name: u?.name || `${u?.prenom || ''} ${u?.nom || ''}`.trim(), role: String(extra.role || u?.role || '').toLowerCase(), team: u?.team || 'Sans équipe', balance: extra.balance ?? u?.balance ?? 0, company: extra.company ?? u?.company ?? null });
const unwrap = (result) => result.status === 'fulfilled' ? result.value : null;

export function AppProvider({ children }) {
  const [user, setUser] = useState(null);
  const [db, setDb] = useState(emptyDB);
  const [authLoading, setAuthLoading] = useState(true);
  const [dataLoading, setDataLoading] = useState(false);
  const [dataError, setDataError] = useState('');
  const [toast, setToast] = useState('');
  const notify = useCallback((text) => {
    setToast(text);
    // Éviter les mises à jour concurrentes n'est pas critique pour une notification.
  }, []);
  useEffect(() => { if (!toast) return; const id = setTimeout(() => setToast(''), 4000); return () => clearTimeout(id); }, [toast]);

  const loadData = useCallback(async (session) => {
    const current = normalizeUser(session.user, { role: session.role, balance: session.balance, company: session.company });
    const role = current.role;
    setDataLoading(true);
    try {
      const jobs = [
        api('/utilisateurs'), api('/demandes'), api('/demandes/mes-demandes'),
        api('/demandes/a-valider'), api('/equipes'), api('/notifications'),
        api('/entreprise'), api('/soldes/mon-solde'),
        role === 'rh' ? api('/invitations') : Promise.resolve([]),
        role === 'rh' ? api('/soldes/entreprise') : api('/soldes/mouvements'),
      ];
      const answers = await Promise.allSettled(jobs);
      if (answers[0].status === 'rejected' || answers[1].status === 'rejected') {
        throw (answers[0].reason || answers[1].reason);
      }
      const [users, requests, mine, pending, teams, notifications, company, balance, invitations, balanceDetails] = answers.map(unwrap);
      const allUsers = Array.isArray(users) ? users.map(item => normalizeUser(item)) : [];
      const myself = allUsers.find(item => item.id === current.id);
      const newUser = normalizeUser({ ...current, ...myself }, { role, balance: balance?.balance ?? myself?.balance ?? session.balance, company: company || session.company });
      setUser(newUser);
      // Un manager peut gérer une équipe sans en être membre : inclure aussi ses demandes personnelles.
      const rows = Array.from(new Map([...(Array.isArray(requests) ? requests : []), ...(Array.isArray(mine) ? mine : [])].map(r => [r.id, r])).values());
      setDb({
        ...emptyDB,
        users: allUsers.length ? allUsers : [newUser], requests: rows,
        mine: Array.isArray(mine) ? mine : rows.filter(r => r.userId === current.id),
        pending: Array.isArray(pending) ? pending : [],
        teams: Array.isArray(teams) ? teams.map(t => ({ ...t, name: t.name || t.nom, manager: t.managerId })) : [],
        notifications: Array.isArray(notifications?.notifications) ? notifications.notifications : [],
        invitations: Array.isArray(invitations) ? invitations.map(i => ({ ...i, code: i.token, expires: i.dateExpiration ? new Date(i.dateExpiration).getTime() : 0, used: i.used ?? i.utilisee })) : [],
        movements: Array.isArray(balanceDetails?.mouvements) ? balanceDetails.mouvements : Array.isArray(balanceDetails) ? balanceDetails : (balance?.mouvements || []),
        company: { ...(company || {}), name: company?.name || company?.nom || '', acquisition: company?.acquisition ?? 2.5 },
        audit: rows.filter(r => r.status !== 'SOUMISE').map(r => ({ id: r.id, date: r.dateDecision || r.created, text: `Demande #${r.id} : ${r.status}${r.comment ? ' — ' + r.comment : ''}` })),
      });
      const failed = answers.find((result, i) => result.status === 'rejected' && ![0, 1].includes(i));
      setDataError(failed ? 'Certaines données sont temporairement indisponibles.' : '');
    } catch (err) {
      setDataError(err.message || 'Erreur de récupération des données.');
      throw err;
    } finally { setDataLoading(false); }
  }, []);

  const refresh = useCallback(async () => {
    const session = await getCurrentUser();
    if (!session.authenticated || !session.user) throw new Error('Session expirée. Reconnectez-vous.');
    await loadData(session);
  }, [loadData]);

  useEffect(() => {
    let valid = true;
    (async () => {
      try {
        const session = await getCurrentUser();
        if (valid && session.authenticated && session.user) await loadData(session);
      } catch (err) {
        if (valid) { setUser(null); if (!/401|Non authentifié/.test(err.message || '')) setDataError(err.message || ''); }
      } finally { if (valid) setAuthLoading(false); }
    })();
    return () => { valid = false; };
  }, [loadData]);

  const login = useCallback(async (userData) => {
    const session = await getCurrentUser();
    if (!session.authenticated) throw new Error('Session non disponible.');
    setUser(normalizeUser(session.user, session));
    await loadData(session);
  }, [loadData]);
  const logout = useCallback(async () => {
    try { await logoutRequest(); }
    finally { setUser(null); setDb(emptyDB); setDataError(''); }
  }, []);
  // Exécute une action serveur, recharge les données et n'affiche le succès qu'après réponse.
  const run = useCallback(async (operation, success = 'Modifications enregistrées.') => {
    try { const result = await operation(); await refresh(); if (success) notify(success); return result; }
    catch (err) { notify(`Erreur : ${err.message || 'Opération refusée'}`); return null; }
  }, [refresh, notify]);

  return <Context.Provider value={{ user, db, authLoading, dataLoading, dataError, login, logout, refresh, run, notify }}>
    {children}
    {toast && <div className="toast" role="status">{toast}</div>}
  </Context.Provider>;
}
export const useApp = () => useContext(Context);
export function workdays(start, end) {
  if (!start || !end || end < start) return 0;
  let days = 0;
  for (const date = new Date(start + 'T12:00:00'); date <= new Date(end + 'T12:00:00'); date.setDate(date.getDate() + 1)) {
    if (date.getDay() !== 0 && date.getDay() !== 6) days++;
  }
  return days;
}
