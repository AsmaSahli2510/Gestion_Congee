
const API_URL = "http://localhost:8080/api/auth";

async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      Accept: "application/json",
      ...options.headers,
    },
  });

  const data = await response.json().catch(() => ({}));

  if (!response.ok) {
    throw new Error(
      data.message || `Erreur HTTP ${response.status}`
    );
  }

  return data;
}

export function loginRequest(email, password) {
  return request("/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email, password }),
  });
}

export function getCurrentUser() {
  return request("/me");
}

export function logoutRequest() {
  return request("/logout", {
    method: "POST",
  });
}

export function registerCompany(data) {
  return request("/register", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  });
}


export function forgotPasswordRequest(email) {
  return request("/mot-de-passe-oublie", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email }),
  });
}
