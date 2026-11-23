import { api, send } from "./api";
export const loginRequest = (email, password) =>
  send("/auth/login", { email, password });
export const getCurrentUser = () => api("/auth/me");
export const logoutRequest = () => api("/auth/logout", { method: "POST" });
export const registerCompany = (data) => send("/auth/register", data);
export const checkInvitation = (token) =>
  api(`/auth/invitation?token=${encodeURIComponent(token)}`);
export const activateInvitation = (data) =>
  send("/auth/activer-invitation", data);
export const forgotPasswordRequest = (email) =>
  send("/auth/mot-de-passe-oublie", { email });
