# Your Car Your Way (YCYW)

Plateforme web centralisée de location de voitures, remplaçant six applications indépendantes (FR, DE, ES, IT, UK, CA, US) par une solution unifiée, internationale et accessible.

## Contexte

YCYW opérait via des applications distinctes par marché (Java EE, PHP Laravel, Node.js, Spring Boot) sans API commune, avec des vulnérabilités de sécurité critiques (SHA-1 pour les mots de passe, TLS 1.0 actif, 41 % de dépendances avec CVE connues) et des performances insuffisantes (97,2 % de disponibilité, jusqu'à 4 % d'erreurs lors des pics).

Ce projet définit l'architecture cible pour remplacer l'ensemble de ces systèmes.

## Stack technique

| Couche | Technologie | Justification |
|---|---|---|
| Frontend | Angular (TypeScript) | Stack validée en prod (US), CDK accessibilité, i18n natif |
| Backend | Spring Boot (Java) | Stack validée en prod (US), maturité entreprise, OpenAPI 3.0 |
| Base de données | PostgreSQL (AWS RDS Multi-AZ) | ACID obligatoire (réservation + paiement) |
| Cache | Redis (AWS ElastiCache) | Sessions JWT révocables, cache recherches |
| Paiement | Stripe | Imposé par le CDC, PCI DSS délégué |
| Authentification | JWT (15 min) + Refresh Token Redis (30 j) | Révocable, scalable horizontalement |
| CI/CD | GitHub Actions | lint → tests → scan sécu → build Docker → deploy AWS |
| Hébergement | AWS (ECS Fargate, RDS, ElastiCache) | Équipes familières, failover Multi-AZ |
| CDN / WAF | Cloudflare | Protection DDoS, cache statique, WAF |

## Architecture

Architecture en couches, API-first. Pas de microservices en V1 (périmètre et taille d'équipe ne le justifient pas), mais préparée pour une extraction future des modules critiques (paiement, notifications).

```
Utilisateur
    │ HTTPS / TLS 1.3
    ▼
CDN Cloudflare (WAF · cache statique · protection DDoS)
    │
    ▼
Frontend Angular (SPA · @ngx-translate · WCAG 2.1 AA)
    │ REST / JSON
    ▼
API Gateway nginx/Kong (JWT · rate limiting · logging)
    │
    ▼
Backend Spring Boot
    ├── Module Auth
    ├── Module Profil
    ├── Module Recherche
    ├── Module Réservation ──► Stripe (Checkout + webhooks HMAC)
    ├── Module Agences
    └── Module Webhook
    │               │
    ▼               ▼
PostgreSQL        Redis
(RDS Multi-AZ)  (ElastiCache)
```

Infrastructure AWS : ALB en subnet public, ECS Fargate + RDS + ElastiCache en subnet privé, secrets dans AWS Secrets Manager, observabilité via CloudWatch + Grafana.

## Fonctionnalités V1

| Bloc | Fonctionnalités |
|---|---|
| SF-01 — Compte | Inscription, connexion/déconnexion, réinitialisation MDP, suppression de compte |
| SF-02 — Profil | Consultation et modification des informations personnelles |
| SF-03 — Recherche | Formulaire (ville départ/retour, dates, catégorie ACRISS), liste et détail des offres |
| SF-04 — Réservation | Réserver, payer via Stripe, historique, modifier (≥ 48h), annuler |
| SF-05 — API agences | CRUD complet exposé aux outils internes (auth par clé API) |
| SF-06 — Tchat support | Session de tchat temps réel via WebSocket/STOMP avec une agence |

**Hors périmètre V1** : application mobile, back-office employés, programme de fidélité, intégration GDS.

## Règles métier clés

- **Modification** : possible jusqu'à 48h avant le départ.
- **Annulation** : remboursement intégral si > 7 jours avant le départ ; 25 % si < 7 jours ; aucun si < 48h.
- **Suppression de compte** : saisie du mot de passe obligatoire ; bloquée si réservation active en cours.
- **Paiement** : aucune donnée de carte bancaire ne transite par les serveurs YCYW (Stripe Checkout hébergé).
- **Catégories véhicules** : norme ACRISS.

## Modèle de données

```
User 1──N Booking N──1 Offer N──1 Agency (départ)
                              N──1 Agency (retour)
```

| Entité | Champs clés |
|---|---|
| `users` | `id`, `email`, `password_hash` (Argon2id), `locale`, `deleted_at` (soft delete RGPD) |
| `agencies` | `id`, `name`, `city`, `country` (ISO 3166-1), `timezone` (IANA) |
| `offers` | `id`, `agency_departure_id`, `agency_return_id`, `vehicle_category` (ACRISS), `price_per_day`, `currency` (ISO 4217) |
| `bookings` | `id`, `user_id`, `offer_id`, `status` (pending/confirmed/cancelled/completed), `stripe_payment_id` |
| `chat_sessions` / `chat_messages` | Sessions de tchat support persistées en base |

## Intégrations tierces

| Service | Usage | Mécanisme |
|---|---|---|
| Stripe | Paiement en ligne | Checkout hébergé + webhooks signés HMAC |
| AWS SES / Resend | Emails transactionnels | Confirmation réservation, réinitialisation MDP |
| API Agences | CRUD interne | Header `X-API-Key`, rate limiting, filtrage par `agency_id` |

## Exigences non fonctionnelles

| Axe | Cible |
|---|---|
| Disponibilité | ≥ 99,5 % (vs 97,2 % actuel) |
| MTTR | < 30 min (vs 2h45 actuel) |
| Performance | p95 < 500 ms, ≥ 500 req/s sans dégradation |
| Taux d'erreur pics | < 0,5 % (vs 4 % actuel) |
| Déploiement | Automatisé, rollback < 5 min |
| Sécurité | 0 CVE critique/élevée, Argon2id, TLS 1.3, secrets managés |
| Accessibilité | WCAG 2.1 AA / RGAA 4.1 |
| i18n | FR, EN, DE, ES, IT minimum |
| Conformité | RGPD (UE), Loi 25 (Québec) |
| Éco-conception | Lighthouse Performance ≥ 85 |

## Sécurité

| Pratique | Implémentation |
|---|---|
| Mots de passe | Argon2id |
| Transport | HTTPS obligatoire, TLS 1.3 |
| Secrets | AWS Secrets Manager, rotation automatique |
| Injection SQL | ORM avec requêtes paramétrées uniquement |
| CSRF | Cookie `SameSite=Strict` + vérification `Origin` |
| Headers HTTP | CSP, X-Frame-Options, HSTS |
| Dépendances | Scan `npm audit` en CI, blocage si CVE critique |
| WebSocket | Authentification JWT à l'établissement de la connexion |

## Documentation

| Document | Description |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | Audit de l'existant, architecture cible, UML complet, modèle de données, choix technologiques |
| [`docs/cahier_des_charges.md`](docs/cahier_des_charges.md) | Spécifications fonctionnelles détaillées, personas, règles métier |
| [`docs/resume.md`](docs/resume.md) | Référence rapide — stack, entités, fonctionnalités, contraintes |
