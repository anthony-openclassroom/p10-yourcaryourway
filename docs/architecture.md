# Proposition d'architecture - Your Car Your Way

## Sommaire

1. [Audit de l'existant](#1-audit-de-lexistant)
2. [Spécifications techniques](#2-spécifications-techniques)
3. [Architecture cible](#3-architecture-cible)
4. [Modélisation UML](#4-modélisation-uml)
5. [Modèle de données](#5-modèle-de-données)
6. [Sélection et justification des technologies](#6-sélection-et-justification-des-technologies)
7. [Intégration des composants tiers](#7-intégration-des-composants-tiers)
8. [Bonnes pratiques](#8-bonnes-pratiques)

---

## 1. Audit de l'existant

### 1.1 Synthèse des applications actuelles

| Pays              | Backend     | Frontend      | Hébergement        | Architecture           |
| ----------------- | ----------- | ------------- | ------------------ | ---------------------- |
| FR / DE / ES / IT | Java EE     | JSP/JSF       | OVH (manuel)       | Monolithe              |
| UK                | PHP Laravel | Laravel Blade | AWS EC2            | Monolithe              |
| CA                | Node.js     | React         | AWS                | Monolithe              |
| US                | Spring Boot | Angular       | Azure App Services | Monolithe containerisé |

### 1.0 Critères d'évaluation

L'audit s'appuie sur les cinq critères suivants, définis en cohérence avec les objectifs de la refonte. La conclusion (§1.4) évalue l'existant au regard de chacun d'eux.

| Critère            | Définition                                                                               | Cible visée                              |
| ------------------ | ---------------------------------------------------------------------------------------- | ---------------------------------------- |
| **Disponibilité**  | Taux d'uptime annuel et temps de récupération après incident (MTTR)                      | ≥ 99,5 %, MTTR < 30 min                  |
| **Sécurité**       | Robustesse face aux CVE, algorithmes de hashage, chiffrement, gestion des secrets        | 0 CVE critique, Argon2id, TLS 1.3        |
| **Performance**    | Capacité de charge (req/s) et taux d'erreur lors des pics saisonniers                    | ≥ 500 req/s, < 0,5 % erreurs             |
| **Maintenabilité** | Cohérence du code, automatisation des déploiements, délai de stabilisation après release | Déploiement automatisé, rollback < 5 min |
| **Évolutivité**    | Capacité à intégrer de nouveaux marchés ou fonctionnalités sans refonte majeure          | Architecture unifiée, API-first          |

---

### 1.2 Forces

- HTTPS activé sur toutes les applications.
- Stack US (Spring Boot / Angular) et CA (Node / React) comme bases de modernisation.
- UK et CA utilisent des algorithmes de hashage conformes (bcrypt, Argon2id).
- Application US containerisée, déploiement reproductible.

### 1.3 Faiblesses

**Disponibilité**

| Environnement     | Disponibilité | MTTR  | Taux déploiements OK |
| ----------------- | ------------- | ----- | -------------------- |
| FR/DE/ES/IT (OVH) | 97,2 %        | ~2h45 | 82 %                 |
| UK/CA (AWS)       | 98,1 – 98,6 % | ~1h10 | 91 %                 |
| US (Azure)        | 98,9 %        | ~1h10 | 91 %                 |

**Sécurité**

| Risque                                     | Niveau   |
| ------------------------------------------ | -------- |
| SHA-1 pour les mots de passe (FR/DE/ES/IT) | Critique |
| TLS 1.0 actif (FR/IT)                      | Critique |
| Secrets en fichiers de configuration       | Élevé    |
| 41 % des dépendances FR avec CVE connues   | Élevé    |

**Performance**

| Environnement | Charge max sans dégradation | Taux d'erreur pics |
| ------------- | --------------------------- | ------------------ |
| FR/DE/ES/IT   | ~150 req/s                  | jusqu'à 4 %        |
| UK            | ~250 req/s                  | 1,5 %              |
| CA            | ~300 req/s                  | 1,5 %              |
| US            | ~350 req/s                  | 0,8 %              |

**Maintenabilité** : 4 codebases sans partage de code, déploiements manuels sur OVH (82 % de réussite), sauvegardes non testées régulièrement.

### 1.4 Conclusion

| Critère        | Évaluation                                                                |
| -------------- | ------------------------------------------------------------------------- |
| Disponibilité  | Non satisfait - FR/DE/ES/IT en dessous de 99 %, MTTR incompatible         |
| Sécurité       | Non satisfait - vulnérabilités critiques sur la majorité des applications |
| Performance    | Non satisfait - aucune application n'atteint la charge cible de 500 req/s |
| Maintenabilité | Non satisfait - 4 codebases sans partage, déploiements manuels            |
| Évolutivité    | Partiellement satisfait - US containerisée, mais aucune API unifiée       |

Une refonte sur une architecture unifiée est justifiée.

---

## 2. Spécifications techniques

| Axe                     | Cible                        |
| ----------------------- | ---------------------------- |
| Disponibilité           | ≥ 99,5 %                     |
| MTTR                    | < 30 minutes                 |
| Charge sans dégradation | ≥ 500 req/s                  |
| Taux d'erreur pics      | < 0,5 %                      |
| Temps de réponse p95    | < 500 ms (pages critiques)   |
| Déploiement             | Automatisé, rollback < 5 min |
| Dépendances vulnérables | 0 CVE critique/élevée        |

**Contraintes** : i18n (FR, EN, DE, ES, IT), WCAG 2.1 AA / RGAA 4.1, RGPD / Loi 25, API RESTful OpenAPI 3.0, paiement délégué à Stripe (zéro donnée CB sur les serveurs YCYW).

---

## 3. Architecture cible

Architecture en couches avec API-first, déployée sur infrastructure cloud managée. Pas de microservices en V1 - la taille de l'équipe et le périmètre ne le justifient pas - mais préparée pour une extraction future des modules critiques (paiement, notifications).

### 3.1 Couches applicatives

```mermaid
flowchart TD
    CDN["<b>CDN / Edge</b> - Cloudflare\nCache statique · WAF · Protection DDoS"]
    FE["<b>Frontend</b> - Angular\nSPA · @ngx-translate · WCAG 2.1 AA"]
    ALB["<b>Load Balancer</b> - AWS ALB\nRoutage · Terminaison TLS 1.3"]
    BE["<b>Backend API</b> - Spring Boot (Java)\nAuth JWT · Rate limiting · Profil · Recherche · Réservation · Agences"]
    STRIPE["<b>Stripe</b>\nPaiement · Webhooks"]
    DB["<b>PostgreSQL</b>\nBase de données principale"]
    REDIS["<b>Redis</b>\nCache sessions · Cache recherches"]

    CDN -->|"HTTPS / TLS 1.3"| FE
    FE -->|"REST / JSON"| ALB
    ALB --> BE
    BE -->|"Checkout session"| STRIPE
    STRIPE -->|"Webhook"| BE
    BE --> DB
    BE --> REDIS
```

### 3.2 Conteneurisation

Le frontend et le backend sont chacun packagés dans une **image Docker distincte**, versionnée et publiée dans AWS ECR à chaque build CI. Cette conteneurisation garantit la reproductibilité des environnements (dev / staging / prod identiques), l'isolation des services et des déploiements sans interruption via ECS Fargate.

### 3.3 Infrastructure cloud (AWS)

**Fournisseur retenu : AWS** (déjà utilisé pour UK/CA, équipes familières). ECS Fargate pour les conteneurs (serverless - pas de gestion de serveurs EC2), RDS PostgreSQL Multi-AZ, ElastiCache Redis, AWS Secrets Manager.

### 3.4 Auto-scaling

L'auto-scaling est géré par **ECS Service Auto Scaling** (Application Auto Scaling d'AWS), qui ajuste dynamiquement le nombre de tâches Fargate (conteneurs) en cours d'exécution en fonction de la charge réelle.

**Mécanisme - Target Tracking Scaling**

La politique retenue est le _Target Tracking_ : ECS maintient automatiquement une métrique cible en ajoutant ou supprimant des tâches.

| Métrique déclencheur       | Seuil cible | Action               |
| -------------------------- | ----------- | -------------------- |
| CPU moyenne des tâches ECS | 70 %        | Scale-out / scale-in |
| Requêtes ALB par cible     | 1 000 req/s | Scale-out / scale-in |

**Configuration min/max (V1)**

| Service  | Tâches min | Tâches max |
| -------- | ---------- | ---------- |
| Backend  | 2          | 10         |
| Frontend | 1          | 5          |

Le minimum de 2 tâches backend garantit la haute disponibilité (réparties sur 2 AZ) même en dehors des pics. Le maximum est fixé selon la capacité RDS Multi-AZ (limitant naturel).

---

## 4. Modélisation UML

### 4.1 Diagramme de cas d'utilisation

```mermaid
flowchart LR
    Client(["👤 Client"])
    Agent(["👤 Agent (agence)"])
    Stripe(["⚙️ Stripe - service externe"])

    subgraph YCYW ["Système YCYW"]
        UC0["S'inscrire"]
        UC1["Se connecter"]
        UC2["Gérer son profil"]
        UC3["Rechercher des offres"]
        UC4["Consulter les agences"]
        UC5["Réserver"]
        UC6["Payer"]
        UC7["API CRUD réservations"]
    end

    Client --> UC0
    Client --> UC1
    Client --> UC2
    Client --> UC3
    Client --> UC4
    Client --> UC5
    UC5 --> UC6
    UC6 --> Stripe
    Agent --> UC7

    UC2 -.->|"<<include>>"| UC1
    UC5 -.->|"<<include>>"| UC1
```

### 4.2 Diagramme de séquence - Réservation

```mermaid
sequenceDiagram
    actor Client
    participant FE as Frontend
    participant BE as API Backend
    participant ST as Stripe
    participant DB

    Client->>FE: Recherche (critères)
    FE->>BE: GET /offers
    BE->>DB: SELECT offers
    DB-->>BE: résultats
    BE-->>FE: liste des offres
    FE-->>Client: affichage des offres

    Client->>FE: Réserver une offre
    FE->>BE: POST /bookings
    BE->>ST: Create PaymentIntent
    ST-->>BE: clientSecret
    BE-->>FE: clientSecret
    FE-->>Client: Stripe Checkout

    Client->>ST: Saisie & validation paiement
    ST-->>Client: Confirmation paiement

    ST->>BE: Webhook payment_intent.succeeded
    BE->>DB: INSERT booking (status = confirmed)
    DB-->>BE: ok
    BE-->>Client: Email de confirmation
```

### 4.3 Diagramme de classes (domaine métier)

```mermaid
classDiagram
    class User {
        id : uuid
        email : string
        passwordHash : string
        firstName : string
        lastName : string
        birthDate : date
        address : string
        locale : string
        createdAt : datetime
        updatedAt : datetime
        deletedAt : datetime
    }

    class Booking {
        id : uuid
        userId : uuid
        offerId : uuid
        status : string
        firstName : string
        lastName : string
        totalAmount : decimal
        currency : string
        stripePaymentId : string
        stripeSessionId : string
        cancelledAt : datetime
        cancellationReason : string
        createdAt : datetime
        updatedAt : datetime
    }

    class Offer {
        id : uuid
        agencyDepartureId : uuid
        agencyReturnId : uuid
        vehicleCategoryCode : string
        startAt : datetime
        endAt : datetime
        pricePerDay : decimal
        currency : string
        availableCount : integer
        createdAt : datetime
    }

    class VehicleCategory {
        code : string
        label : string
        description : string
        seats : integer
        doors : integer
        luggageLarge : integer
        luggageSmall : integer
        transmission : string
        airConditioning : boolean
        fuelType : string
        imageUrl : string
        createdAt : datetime
    }

    class Agency {
        id : uuid
        name : string
        city : string
        country : string
        address : string
        timezone : string
        latitude : decimal
        longitude : decimal
        createdAt : datetime
    }

    class Vehicle {
        id : uuid
        agencyId : uuid
        vehicleCategoryCode : string
        vin : string
        licensePlate : string
        make : string
        model : string
        year : integer
        color : string
        mileageKm : integer
        status : string
        createdAt : datetime
        updatedAt : datetime
    }

    User "1" --> "*" Booking : possède
    Booking "*" --> "1" Offer : porte sur
    Offer "*" --> "1" Agency : départ
    Offer "*" --> "1" Agency : retour
    Offer "*" --> "1" VehicleCategory : classifie
    Agency "1" --> "*" Vehicle : possède
    Vehicle "*" --> "1" VehicleCategory : appartient à
```

### 4.4 Diagramme de déploiement

```mermaid
flowchart TD
    Users(["👤 Utilisateurs"])
    Stripe(["💳 Stripe"])

    subgraph AWS ["AWS Cloud"]
        subgraph VPC ["VPC"]
            subgraph Public ["Subnet public"]
                ALB["ALB\nHTTPS"]
            end
            subgraph Private ["Subnet privé"]
                ECS_BE["ECS Fargate\nBackend API"]
                ECS_FE["ECS Fargate\nFrontend"]
                RDS["RDS PostgreSQL\nMulti-AZ"]
                Redis["ElastiCache\nRedis"]
            end
        end
        Secrets["Secrets Manager"]
        CW["CloudWatch\n+ Grafana"]
    end

    Users -->|HTTPS / TLS 1.3| ALB
    ALB --> ECS_BE
    ALB --> ECS_FE
    ECS_BE --> RDS
    ECS_BE --> Redis
    ECS_BE -.-> Secrets
    ECS_BE -.-> CW
    Stripe -->|Webhooks HMAC| ECS_BE
```

### 4.5 Diagramme de composants

```mermaid
flowchart TB
    subgraph DOCKER_FE ["Conteneur Docker - image frontend"]
        subgraph FE ["Frontend - Angular"]
            P1["Page Accueil / Recherche"]
            P2["Page Résultats"]
            P3["Page Réservation"]
            P4["Page Profil"]
            P5["Page Historique"]
            P6["Page Tchat (SF-06) ⭐ PoC"]
        end
    end

    subgraph DOCKER_BE ["Conteneur Docker - image backend"]
        subgraph BE ["Backend API - Spring Boot"]
            M1["Module Auth"]
            M2["Module Profil"]
            M3["Module Recherche"]
            M4["Module Réservation"]
            M5["Module Agences"]
            M6["Module Webhook"]
            M7["Module Tchat (SF-06) ⭐ PoC\nREST + WebSocket/STOMP"]
        end
    end

    subgraph EXT ["Services externes"]
        STRIPE["Stripe"]
        EMAIL["Service Email (AWS SES)"]
    end

    DB[("PostgreSQL\nAWS RDS")]
    CACHE[("Redis\nAWS ElastiCache")]

    P1 -->|"GET /offers"| M3
    P3 -->|"POST /bookings"| M4
    P4 -->|"GET/PATCH /users/:id"| M2
    P5 -->|"GET /bookings"| M4
    P1 & P4 -->|"POST /auth"| M1
    P6 -->|"REST + STOMP/WebSocket"| M7

    M4 -->|"createPaymentIntent"| STRIPE
    M6 -->|"webhook"| STRIPE
    M4 & M1 -->|"sendEmail"| EMAIL

    M1 & M2 & M3 & M4 & M5 --> DB
    M7 -->|"chat_sessions, chat_messages"| DB
    M1 & M3 --> CACHE
```

### 4.6 Diagramme de classes - Module Tchat (SF-06)

```mermaid
classDiagram
    class ChatSession {
        id : uuid
        userId : uuid
        agencyId : uuid
        status : string
        createdAt : datetime
        closedAt : datetime
    }

    class ChatMessage {
        id : uuid
        sessionId : uuid
        senderRole : string
        content : string
        sentAt : datetime
    }

    ChatSession "1" --> "*" ChatMessage : contient
```

> `status` : `open` | `closed` - `senderRole` : `client` | `agent` | `system`

> Les FK vers `users(id)` et `agencies(id)` sont présentes dans le schéma cible. Dans le PoC, elles sont simulées par des UUIDs fixes (`DEMO_USER_ID`, `DEMO_AGENCY_ID`) en l'absence des modules Auth et Agences.

---

## 5. Modèle de données

### 5.1 Tables principales (PostgreSQL)

```sql
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,           -- Argon2id
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    birth_date    DATE,
    address       TEXT,
    locale        VARCHAR(10) DEFAULT 'fr', -- BCP 47
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ             -- soft delete RGPD
);

CREATE TABLE agencies (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    country    CHAR(2) NOT NULL,           -- ISO 3166-1 alpha-2
    address    TEXT,
    timezone   VARCHAR(50) NOT NULL,       -- IANA tz
    latitude   NUMERIC(9,6),
    longitude  NUMERIC(9,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE vehicle_categories (
    code             CHAR(4) PRIMARY KEY,           -- code ACRISS (ex: CDAR, SWAR)
    label            VARCHAR(100) NOT NULL,          -- ex: "Compact 4 portes"
    description      TEXT,
    seats            INTEGER NOT NULL,
    doors            INTEGER,
    luggage_large    INTEGER NOT NULL DEFAULT 0,
    luggage_small    INTEGER NOT NULL DEFAULT 0,
    transmission     VARCHAR(10) NOT NULL            -- 'manual' | 'automatic'
                     CONSTRAINT vc_transmission_check CHECK (transmission IN ('manual', 'automatic')),
    air_conditioning BOOLEAN NOT NULL DEFAULT TRUE,
    fuel_type        VARCHAR(10) NOT NULL DEFAULT 'petrol'
                     CONSTRAINT vc_fuel_check CHECK (fuel_type IN ('petrol', 'diesel', 'hybrid', 'electric')),
    image_url        TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE offers (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_departure_id    UUID NOT NULL REFERENCES agencies(id),
    agency_return_id       UUID NOT NULL REFERENCES agencies(id),
    vehicle_category_code  CHAR(4) NOT NULL REFERENCES vehicle_categories(code),
    start_at               TIMESTAMPTZ NOT NULL,
    end_at                 TIMESTAMPTZ NOT NULL,
    price_per_day          NUMERIC(10,2) NOT NULL,
    currency               CHAR(3) NOT NULL DEFAULT 'EUR', -- ISO 4217
    available_count        INTEGER NOT NULL DEFAULT 1,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT offers_dates_check CHECK (end_at > start_at)
);

CREATE TABLE vehicles (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id             UUID NOT NULL REFERENCES agencies(id),
    vehicle_category_code CHAR(4) NOT NULL REFERENCES vehicle_categories(code),
    vin                   CHAR(17) NOT NULL UNIQUE,    -- identifiant mondial du châssis
    license_plate         VARCHAR(20) NOT NULL,         -- unique par pays (non contraint globalement)
    make                  VARCHAR(100) NOT NULL,         -- ex: Renault
    model                 VARCHAR(100) NOT NULL,         -- ex: Clio
    year                  SMALLINT NOT NULL,
    color                 VARCHAR(50),
    mileage_km            INTEGER NOT NULL DEFAULT 0,
    status                VARCHAR(15) NOT NULL DEFAULT 'available'
                          CONSTRAINT vehicles_status_check
                          CHECK (status IN ('available', 'rented', 'maintenance', 'retired')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL REFERENCES users(id),
    offer_id           UUID NOT NULL REFERENCES offers(id),
    status             VARCHAR(20) NOT NULL DEFAULT 'pending',
                       -- pending | confirmed | cancelled | completed
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    total_amount       NUMERIC(10,2) NOT NULL,
    currency           CHAR(3) NOT NULL,
    stripe_payment_id  VARCHAR(255),
    stripe_session_id  VARCHAR(255),
    cancelled_at       TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Filtrage par pays : offers → agencies.country (via agency_departure_id)
-- ex: SELECT o.* FROM offers o JOIN agencies a ON o.agency_departure_id = a.id WHERE a.country = 'FR'
CREATE INDEX idx_agencies_country      ON agencies(country);
CREATE INDEX idx_bookings_user_id      ON bookings(user_id);
CREATE INDEX idx_bookings_offer_id     ON bookings(offer_id);
CREATE INDEX idx_offers_departure      ON offers(agency_departure_id, start_at);
CREATE INDEX idx_offers_category       ON offers(vehicle_category_code);
CREATE INDEX idx_vehicles_agency       ON vehicles(agency_id);
CREATE INDEX idx_vehicles_category     ON vehicles(vehicle_category_code);
```

### 5.2 Tables de tchat (module SF-06)

> **POC** : le module de tchat est développé de manière isolée. Les FK vers `users(id)` et `agencies(id)` sont présentes dans le schéma cible ci-dessous, mais absentes de la migration du POC car ces tables n'existent pas encore. Les IDs sont simulés avec des UUIDs fixes en dur (`DEMO_USER_ID`, `DEMO_AGENCY_ID`) le temps que les modules Auth et Agences soient implémentés.

```sql
CREATE TABLE chat_sessions (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id),
    agency_id  UUID NOT NULL REFERENCES agencies(id),
    status     VARCHAR(10) NOT NULL DEFAULT 'open'
               CONSTRAINT chat_sessions_status_check CHECK (status IN ('open', 'closed')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at  TIMESTAMPTZ
);

CREATE TABLE chat_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_role VARCHAR(10) NOT NULL
                CONSTRAINT chat_messages_role_check CHECK (sender_role IN ('client', 'agent')),
    content     TEXT NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_sessions_user_id    ON chat_sessions(user_id);
```

### 5.3 Statuts de réservation

| Statut      | Description                   |
| ----------- | ----------------------------- |
| `pending`   | Paiement initié, non confirmé |
| `confirmed` | Paiement validé par Stripe    |
| `cancelled` | Annulée par le client         |
| `completed` | Location terminée             |

### 5.4 Stratégie de migration

Déploiement progressif par marché (UK en premier), avec import des données historiques et run parallèle avant décommissionnement des anciens systèmes.

---

## 6. Sélection et justification des technologies

### 6.1 Frontend - Angular

| Critère                | Angular           | Next.js (React) | Vue.js / Nuxt |
| ---------------------- | ----------------- | --------------- | ------------- |
| TypeScript             | Natif             | Natif           | Optionnel     |
| i18n intégré           | ✅ @ngx-translate | ✅ next-intl    | ✅ vue-i18n   |
| SSR                    | ✅ Angular SSR    | ✅ natif        | ✅ Nuxt       |
| Accessibilité (CDK)    | ✅ Angular CDK    | Radix UI        | Headless UI   |
| Déjà utilisé chez YCYW | États-Unis        | Canada          | Non           |

**Choix retenu : Angular.** Stack déjà en production aux États-Unis, structure stricte adaptée à une application d'entreprise, TypeScript natif, Angular CDK pour l'accessibilité.

### 6.2 Backend - Spring Boot (Java)

| Critère                | Spring Boot        | NestJS              | Express.js |
| ---------------------- | ------------------ | ------------------- | ---------- |
| Structure              | Opinionée (Spring) | Opinionée (modules) | Libre      |
| OpenAPI auto-généré    | ✅ Springdoc       | ✅ @nestjs/swagger  | Manuel     |
| Maturité / robustesse  | Très élevée        | Élevée              | Moyenne    |
| Déjà utilisé chez YCYW | États-Unis         | Non                 | Canada     |

**Choix retenu : Spring Boot.** Référence en environnement d'entreprise, déjà en production US, documentation API auto-générée via Springdoc (OpenAPI 3.0).

### 6.3 Base de données - PostgreSQL

| Critère                       | PostgreSQL | MySQL         | MongoDB |
| ----------------------------- | ---------- | ------------- | ------- |
| Transactions ACID             | ✅         | ✅            | Partiel |
| Modèle relationnel            | ✅         | ✅            | ❌      |
| UUID natif                    | ✅         | Via extension | ✅      |
| Adapté réservations/paiements | ✅         | ✅            | ❌      |

**Choix retenu : PostgreSQL.** Modèle fortement relationnel, transactions ACID indispensables entre réservation et confirmation de paiement.

### 6.4 Cache - Redis

Stockage des sessions JWT avec révocation immédiate (suppression de compte, déconnexion forcée) et cache des résultats de recherche pour absorber les pics saisonniers.

### 6.5 Paiement - Stripe

Imposé par le cahier des charges. Stripe Checkout (page hébergée) : aucune donnée CB sur les serveurs YCYW, conformité PCI DSS déléguée. Webhooks pour la confirmation asynchrone.

### 6.6 Authentification - JWT + Redis

| Critère                      | JWT seul | Sessions serveur | JWT + Refresh Redis |
| ---------------------------- | -------- | ---------------- | ------------------- |
| Scalabilité horizontale      | ✅       | ❌               | ✅                  |
| Révocation immédiate         | ❌       | ✅               | ✅                  |
| Adapté suppression de compte | ❌       | ✅               | ✅                  |

**Choix retenu :** Access Token JWT (15 min) + Refresh Token révocable en Redis (30 jours).

### 6.7 CI/CD - GitHub Actions

Pipeline : lint → tests → scan sécurité → build image Docker → déploiement AWS.

---

## 7. Intégration des composants tiers

### 7.1 Stripe

```mermaid
sequenceDiagram
    actor Client
    participant BE as Backend
    participant ST as Stripe

    Client->>BE: POST /bookings
    BE->>ST: createPaymentIntent
    ST-->>BE: clientSecret
    BE-->>Client: redirect Stripe Checkout

    Client->>ST: Complète le paiement
    ST-->>Client: Confirmation

    ST->>BE: Webhook POST /webhooks/stripe<br/>(payment_intent.succeeded)
    BE->>BE: UPDATE booking status = confirmed
    BE-->>Client: Email de confirmation
```

Vérification de la signature webhook via `stripe.webhooks.constructEvent()` - toute requête sans signature valide est rejetée (HTTP 400).

### 7.2 Service d'email - AWS SES

```mermaid
sequenceDiagram
    participant BE as Backend (Spring Boot)
    participant SES as AWS SES
    participant User as Utilisateur

    Note over BE,User: Confirmation de réservation
    BE->>SES: sendEmail(to, template, variables)
    SES-->>User: Email HTML (confirmation + récapitulatif)

    Note over BE,User: Réinitialisation de mot de passe
    BE->>SES: sendEmail(to, reset-link, expiry=1h)
    SES-->>User: Email HTML (lien sécurisé)
```

**Mécanisme** : l'email est envoyé de façon synchrone après confirmation du webhook Stripe (`payment_intent.succeeded`) pour les réservations, et directement depuis l'endpoint `POST /auth/forgot-password` pour les réinitialisations. Les templates HTML sont stockés côté backend. AWS SES est configuré avec un domaine vérifié (SPF, DKIM) pour éviter le classement en spam.

### 7.3 API Agences

```mermaid
flowchart LR
    APP["Application Agence"]
    ALB["AWS ALB"]
    BE["Backend API\nValidation X-API-Key · Rate limit · CRUD"]
    DB["PostgreSQL"]

    APP -->|"X-API-Key"| ALB
    ALB --> BE
    BE --> DB
```

Clé API stockée dans AWS Secrets Manager. La validation de la clé et le rate limiting sont assurés par le Backend (Spring Boot). Opérations filtrées par `agency_id` (row-level filtering).

---

## 8. Bonnes pratiques

### 8.1 Sécurité

| Pratique      | Implémentation                                  |
| ------------- | ----------------------------------------------- |
| Mots de passe | Argon2id                                        |
| Transport     | HTTPS obligatoire, TLS 1.3                      |
| Secrets       | AWS Secrets Manager, jamais en dur dans le code |
| Headers HTTP  | CSP, X-Frame-Options, HSTS                      |
| Injection SQL | ORM avec requêtes paramétrées uniquement        |
| CSRF          | Cookie SameSite=Strict + vérification Origin    |
| Dépendances   | Scan `npm audit` en CI, blocage si CVE critique |

### 8.2 Accessibilité

| Critère              | Mesure                                                             |
| -------------------- | ------------------------------------------------------------------ |
| Structure sémantique | HTML5 natif (`<main>`, `<nav>`, `<section>`, `<article>`)          |
| Navigation clavier   | Focus visible, pas de pièges au clavier                            |
| Images               | `alt` descriptif sur images porteuses, `alt=""` sur décoratives    |
| Formulaires          | `<label>` explicite, `aria-describedby` pour erreurs               |
| Contraste            | ≥ 4,5:1 (texte normal), ≥ 3:1 (texte large) - vérifié par axe-core |
| Lecteurs d'écran     | `role="alert"` pour les messages dynamiques                        |
| Validation           | Audit automatisé `axe-core` en CI + revue manuelle trimestrielle   |

### 8.3 Impact écologique

| Pratique        | Détail                                                    |
| --------------- | --------------------------------------------------------- |
| Assets          | WebP/AVIF, lazy loading natif                             |
| Bundle          | Lazy-loading de modules Angular, code splitting par route |
| Requêtes réseau | Cache HTTP (ETag, Cache-Control), pagination serveur      |
| Hébergement     | AWS régions européennes (énergie renouvelable ~100 %)     |
| Monitoring      | Lighthouse Performance ≥ 85 requis en CI                  |
| Base de données | Indexes optimisés, requêtes N+1 interdites                |
