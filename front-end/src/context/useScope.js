import { useApp } from './AppContext';
export default function useScope() {
  const { user, db } = useApp();
  if (!user) return { members: [], requests: [], mine: [], pending: [] };
  const members = user.role === 'employe' ? db.users.filter(u => u.id === user.id || u.team === user.team) : db.users;
  return { members, requests: db.requests, mine: db.mine, pending: db.pending };
}
