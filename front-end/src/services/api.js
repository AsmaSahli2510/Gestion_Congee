/** Client HTTP same-origin : Vite transmet /api à Spring Boot (session JSESSIONID). */
export async function api(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const headers = { Accept: "application/json", ...options.headers };
  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    // Spring Security exige un jeton CSRF pour les modifications et la connexion.
    let csrfResponse;
    try {
      csrfResponse = await fetch("/api/auth/csrf", { credentials: "include" });
    } catch {
      throw new Error(
        "Serveur inaccessible. Démarrez Spring Boot sur le port 8080.",
      );
    }
    if (!csrfResponse.ok)
      throw new Error("Impossible de préparer la session sécurisée.");
    const csrf = await csrfResponse.json();
    headers[csrf.headerName] = csrf.token;
  }
  if (
    options.body &&
    !(options.body instanceof FormData) &&
    !headers["Content-Type"]
  ) {
    headers["Content-Type"] = "application/json";
  }
  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      method,
      headers,
      credentials: "include",
    });
  } catch {
    throw new Error(
      "Serveur inaccessible. Vérifiez Spring Boot sur le port 8080.",
    );
  }
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const violations =
      data.errors &&
      (Array.isArray(data.errors) ? data.errors : Object.values(data.errors));
    throw new Error(
      data.message ||
        violations?.join(", ") ||
        `Erreur HTTP ${response.status}`,
    );
  }
  return data;
}
export const send = (path, body, method = "POST") =>
  api(path, { method, body: JSON.stringify(body) });
