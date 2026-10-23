
import { useApp } from "./AppContext";

export default function useScope() {
  const { user } = useApp();

  // Éviter les erreurs si les données ne sont pas disponibles
  if (!user) {
    return {
      members: [],
      requests: [],
      mine: [],
      pending: [],
    };
  }

  // Le contexte actuel ne fournit pas encore db.users
  // ni db.requests. On utilise donc des listes vides
  // jusqu'à la connexion des données au backend.
  const members = [];
  const requests = [];
  const mine = [];
  const pending = [];

  return {
    members,
    requests,
    mine,
    pending,
  };
}
