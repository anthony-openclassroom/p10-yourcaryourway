# YCYW — Tchat Support (POC SF-06)

Proof of concept du module **SF-06 — Tchat support** de la plateforme **Your Car Your Way** — une messagerie temps réel entre un client et un agent support via WebSocket/STOMP.

> Ce dépôt couvre uniquement le module de tchat. L'architecture complète de la plateforme est documentée dans [`docs/architecture.md`](docs/architecture.md).

---

## Stack technique

| Couche            | Technologie       | Version |
| ----------------- | ----------------- | ------- |
| Frontend          | Angular + SSR     | 20.x    |
| Backend           | Spring Boot       | 4.1.0   |
| Langage backend   | Java              | 21      |
| Base de données   | PostgreSQL        | 16      |
| Cache             | Redis             | 7       |
| Migrations        | Flyway            | —       |
| WebSocket         | STOMP over SockJS | —       |
| Documentation API | SpringDoc OpenAPI | 2.5.0   |

---

## Structure du projet

```
./
├── backend/                    # Spring Boot API
│   ├── src/
│   │   ├── main/java/com/ycyw/chat/
│   │   │   ├── config/         # WebSocket, CORS
│   │   │   ├── controller/     # REST + WebSocket controllers
│   │   │   ├── dto/            # Request / Response DTOs
│   │   │   ├── model/          # Entités JPA
│   │   │   ├── repository/     # Spring Data JPA
│   │   │   └── service/        # Logique métier
│   │   ├── main/resources/
│   │   │   ├── application.properties
│   │   │   ├── application-dev.properties
│   │   │   └── db/migration/   # Scripts Flyway
│   │   └── test/               # Tests unitaires (Mockito)
│   ├── .env.sample             # Template des variables d'environnement
│   └── pom.xml
│
├── frontend/                   # Angular 20 + SSR
│   ├── src/
│   │   ├── app/
│   │   │   ├── chat/           # Feature module — tchat
│   │   │   │   ├── chat/       # Composant, service, modèles
│   │   │   │   ├── chat-module.ts
│   │   │   │   └── chat-routing-module.ts
│   │   │   ├── home/           # Page d'accueil
│   │   │   ├── app-module.ts
│   │   │   └── app-routing-module.ts
│   │   ├── environments/
│   │   │   ├── environment.ts              # Production
│   │   │   └── environment.development.ts  # Développement
│   │   └── styles.scss         # Variables CSS globales (couleurs de marque)
│   └── package.json
│
├── docs/                       # Documentation projet
│   ├── architecture.md
│   ├── cahier_des_charges.md
│   └── resume.md
└── docker-compose.yml          # PostgreSQL + Redis
```

---

## Prérequis

- **Java 21** — [sdkman.io](https://sdkman.io) recommandé
- **Node.js 20+** + npm
- **Docker & Docker Compose**

---

## Installation

### 1. Cloner le dépôt

```bash
git clone <url-du-repo>
cd ycyw
```

### 2. Démarrer les services (base de données + Redis)

```bash
docker compose up -d
```

PostgreSQL sera disponible sur `localhost:5433`, Redis sur `localhost:6379`.

### 3. Configurer le backend

```bash
cd backend
cp .env.sample .env
```

Éditer `.env` si nécessaire (les valeurs par défaut fonctionnent avec le `docker-compose.yml`) :

```env
SPRING_PROFILES_ACTIVE=dev

DATABASE_URL=jdbc:postgresql://localhost:5433/ycyw_dev
DATABASE_USERNAME=ycyw
DATABASE_PASSWORD=dev_password

FRONTEND_URL=http://localhost:4200
```

### 4. Installer les dépendances frontend

```bash
cd ../frontend
npm install
```

---

## Lancer le projet

Ouvrir **deux terminaux**.

**Terminal 1 — Backend**

```bash
cd backend
./mvnw spring-boot:run
```

API disponible sur `http://localhost:8081`
Swagger UI sur `http://localhost:8081/swagger-ui.html`

**Terminal 2 — Frontend**

```bash
cd frontend
npm start
```

Application disponible sur `http://localhost:4200`

---

## Utiliser le tchat (mode démo)

Le POC simule une session entre deux rôles via deux onglets. Aucune authentification n'est requise.

1. Ouvrir `http://localhost:4200` → cliquer **Contacter le support**
2. Une session est créée automatiquement et l'URL contient `?sessionId=...&role=client`
3. Copier le **lien agent** affiché dans le header du tchat
4. Ouvrir ce lien dans un **second onglet** → vue agent active
5. Les deux onglets communiquent en temps réel via WebSocket

---

## API REST

Base URL : `http://localhost:8081/api`

| Méthode | Endpoint                       | Description                         |
| ------- | ------------------------------ | ----------------------------------- |
| `POST`  | `/chat/sessions`               | Créer une session de tchat          |
| `GET`   | `/chat/sessions/{id}/messages` | Récupérer l'historique des messages |
| `PATCH` | `/chat/sessions/{id}/close`    | Fermer une session                  |

La documentation complète (schémas, exemples) est disponible sur **Swagger UI** :
`http://localhost:8081/swagger-ui.html`

---

## WebSocket (STOMP -> Simple Text Oriented Messaging Protocol)

Point de connexion : `http://localhost:8081/ws-sockjs` (SockJS fallback activé)

| Type          | Destination               | Description                         |
| ------------- | ------------------------- | ----------------------------------- |
| **Subscribe** | `/topic/chat/{sessionId}` | Recevoir les messages d'une session |
| **Publish**   | `/app/chat/{sessionId}`   | Envoyer un message                  |

**Format du message envoyé :**

```json
{
	"content": "Bonjour, j'ai besoin d'aide.",
	"senderRole": "client"
}
```

**Format du message reçu :**

```json
{
	"id": "uuid",
	"sessionId": "uuid",
	"senderRole": "client",
	"content": "Bonjour, j'ai besoin d'aide.",
	"sentAt": "2026-01-15T10:30:00Z"
}
```

---

## Base de données

Les migrations sont gérées par **Flyway** et s'exécutent automatiquement au démarrage du backend.

```sql
chat_sessions
  id          UUID PK
  user_id     UUID          -- ID client (auth externe)
  agency_id   UUID          -- ID agence (auth externe)
  status      VARCHAR(10)   -- 'open' | 'closed'
  created_at  TIMESTAMPTZ
  closed_at   TIMESTAMPTZ

chat_messages
  id          UUID PK
  session_id  UUID FK → chat_sessions(id)
  sender_role VARCHAR(10)   -- 'client' | 'agent'
  content     TEXT
  sent_at     TIMESTAMPTZ
```

---

## Tests

```bash
cd backend
./mvnw test
```

Les tests utilisent **H2 en mémoire** — aucune base externe requise. Flyway est désactivé pour les tests (schéma créé par `ddl-auto=create-drop`).

---

## Variables d'environnement — référence complète

| Variable                 | Défaut (`dev`)                              | Description                  |
| ------------------------ | ------------------------------------------- | ---------------------------- |
| `SPRING_PROFILES_ACTIVE` | `dev`                                       | Profil Spring actif          |
| `DATABASE_URL`           | `jdbc:postgresql://localhost:5433/ycyw_dev` | URL JDBC PostgreSQL          |
| `DATABASE_USERNAME`      | `ycyw`                                      | Utilisateur base de données  |
| `DATABASE_PASSWORD`      | `dev_password`                              | Mot de passe base de données |
| `FRONTEND_URL`           | `http://localhost:4200`                     | URL du frontend (CORS)       |

---

## Documentation

| Document                                                   | Contenu                                                                  |
| ---------------------------------------------------------- | ------------------------------------------------------------------------ |
| [`docs/architecture.md`](docs/architecture.md)             | Architecture cible complète, audit de l'existant, UML, modèle de données |
| [`docs/cahier_des_charges.md`](docs/cahier_des_charges.md) | Spécifications fonctionnelles, personas, règles métier                   |
| [`docs/resume.md`](docs/resume.md)                         | Référence rapide — stack, entités, fonctionnalités, contraintes          |
