# LeaveFlow — React + Vite

Application statique de gestion des congés, en français, avec les interfaces RH, Manager et Employé du cahier des charges. Le login et le signup reprennent le design réalisé, le logo fourni et la palette officielle.

## Démarrer sous Windows / VS Code

Installez Node.js 20.19+ ou 22+, extrayez ce ZIP, ouvrez le dossier `LeaveFlow_React_Vite` dans VS Code, puis lancez :

```bash
npm install
npm run dev
```

Ouvrez l’adresse affichée par Vite. Pour arrêter : `Ctrl + C` dans le terminal.

```bash
npm run build
npm run preview
```

`build` génère le dossier `dist`. Utilisez un serveur HTTP : ne double-cliquez pas sur `dist/index.html`. Sur un hébergement, configurez le renvoi des routes vers `index.html` (SPA).

## Explorer les trois rôles

Sur la page Login, les boutons **RH**, **Manager**, **Employé** ouvrent directement les espaces de démonstration.

| Rôle | Email | Mot de passe démo |
| --- | --- | --- |
| RH | rh@demo.fr | Demo2026! |
| Manager | manager@demo.fr | Demo2026! |
| Employé | employe@demo.fr | Demo2026! |

Les mots de passe saisis à l’inscription ne sont pas stockés. Tout nouveau compte de simulation peut être retrouvé via les boutons de démonstration ; la connexion manuelle utilise toujours `Demo2026!`.

## Structure

- `src/pages/` : chaque écran dans son fichier JSX.
- `src/components/` : layout, sidebar, composants d’authentification, tables et éléments réutilisables.
- `src/images/` : logo complet et icône du projet.
- `src/context/` : données partagées, stockage local, session simulée et périmètres de rôle.
- `src/data/seed.js` : données de démonstration réinitialisables.
- `src/App.jsx` : imports et déclaration des routes uniquement.
- `src/main.jsx` : point d’entrée, routeur, provider et styles.
- `src/styles.css` : thème de l’application et responsive.
- `src/auth.css` : thème du login/signup conservé.

## Interfaces incluses

**Public** : connexion unique, signup avec deux cartes, création d’entreprise RH, activation d’invitation, mot de passe oublié et page 404.

**RH** : dashboard société, informations entreprise, équipes et managers, employés, invitations, demandes à valider, demandes personnelles, formulaire de demande/modification, détail et statut, calendrier société, soldes et ajustements, journal des mouvements, statistiques, exports, historique des décisions, notifications, profil et règles/types de congés.

**Manager** : dashboard équipe, membres, validation des demandes des employés de l’équipe, calendrier équipe, soldes équipe, statistiques et exports, demandes personnelles validées par un RH, historique, notifications et profil.

**Employé** : dashboard personnel, nouvelle demande, modification et annulation avant décision, détail des demandes, historique, solde personnel, journal, calendrier de ses demandes, notifications et profil.

## Parcours de test

1. Entrez comme Employé : créez une demande sur des dates sans chevauchement.
2. Déconnectez-vous, entrez comme Manager : examinez la demande dans « Demandes à valider ».
3. Approuvez : le solde est débité une seule fois. Pour rejeter, un commentaire est requis.
4. Revenez comme Employé : retrouvez le statut et la notification.
5. Entrez comme RH : créez une équipe, transférez un collaborateur, nommez un manager, ajustez un solde avec un motif et créez une invitation.
6. Copiez le code d’invitation, déconnectez-vous puis ouvrez Sign up → Rejoindre mon équipe. Utilisez exactement l’email et le code. Le code expire après 7 jours et ne s’utilise qu’une fois dans les données locales.
7. Exportez les tableaux en CSV. « PDF / imprimer » ouvre l’impression du navigateur ; choisissez « Enregistrer au format PDF ».

## Ce que simule la version statique

Les modifications sont conservées dans `localStorage` du navigateur sur la même origine. Aucun serveur, aucune base distante, aucun email envoyé. Le prototype n’offre pas une authentification sécurisée. Les gardes de routes servent à explorer les interfaces ; les droits et `entreprise_id` doivent être contrôlés côté serveur en Java EE.

Les justificatifs conservent seulement leur nom, pas leurs fichiers. Les notifications sont internes et locales. Le crédit mensuel, les rappels et la récupération de mot de passe ne s’exécutent pas automatiquement. Le calendrier travaille sur les dates des demandes ; le calcul de durée exclut les weekends, mais ne gère pas les jours fériés.

Créer une nouvelle entreprise démarre un nouvel espace local vide et remplace l’organisation de démonstration du navigateur. Ce comportement sert à simuler l’onboarding, pas un véritable hébergement multi-entreprises. RH → Règles & paramètres → Restaurer les données d’exemple remet les comptes initiaux.

Un manager ne valide pas ses propres demandes. Les demandes personnelles RH nécessitent un autre RH : invitez-en un pour tester ce parcours. Les statistiques indiquent les valeurs réellement calculables avec les données locales, sans inventer un taux d’absence.

## Palette

| Nom | Hex |
| --- | --- |
| Night Skies | #000181 |
| Blue Raspberry | #006EE9 |
| Violet | #E8A0FF |
| Powdered Lime | #D0FFA4 |
| Sunny Skies | #83E7FF |

## Vérification effectuée

Compilation production Vite réussie. Rendu React de 44 routes avec données de démonstration et vérification du calcul des jours ouvrés. Aucun test visuel dans un navigateur effectué dans cet environnement.
