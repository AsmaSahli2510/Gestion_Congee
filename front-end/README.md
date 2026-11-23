# LeaveFlow React

Voir [README.md à la racine](../README.md) pour les instructions complètes.

```bash
npm ci
npm run dev
```

Interface : http://localhost:5173. Proxy `/api` → Spring Boot sur http://localhost:8080.
Le frontend utilise la session et un token CSRF, pas localStorage.
