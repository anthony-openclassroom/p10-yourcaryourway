# Cahier des charges — Your Car Your Way

## Objet du document

Ce cahier des charges définit les exigences fonctionnelles, les besoins utilisateurs et les contraintes réglementaires de la nouvelle application centralisée Your Car Your Way. Il consolide les besoins métier existants, les complète et intègre les exigences d'accessibilité conformément au référentiel RGAA 4.1.

---

## Contexte

Your Car Your Way est une entreprise de location de voitures présente depuis plus de vingt ans en Europe, récemment implantée en Amérique du Nord. Elle opère actuellement via plusieurs applications distinctes (France, Allemagne, Espagne, Italie, Royaume-Uni, Canada, États-Unis), développées indépendamment avec des technologies hétérogènes.

Cette fragmentation génère :

- Une complexité technique croissante (4 stacks différentes, aucune API unifiée).
- Des incohérences fonctionnelles entre marchés (règles métier locales divergentes).
- Des difficultés de maintenance et de sécurité (41 % des dépendances FR avec CVE connues, SHA-1 pour les mots de passe).

L'objectif est de remplacer l'ensemble de ces applications par une plateforme centralisée, internationale, accessible et maintenable.

---

## Périmètre

### Inclus dans la V1

- Portail client web accessible depuis tous les marchés.
- Gestion du profil utilisateur.
- Recherche et réservation de véhicules.
- Paiement en ligne via fournisseur externe (Stripe).
- API RESTful consommée par les applications d'agence (usage interne CRUD).

### Hors périmètre V1

- Application mobile native.
- Back-office employés (gestion de flotte, planning agence).
- Programme de fidélité.
- Intégration de systèmes GDS (Global Distribution System).

---

## Parties prenantes

| Rôle | Implication |
|---|---|
| Client final | Utilisateur principal de l'application web |
| Agent en agence | Consommateur de l'API interne (lecture/modification réservations) |
| Équipe DSI | Exploitation, sécurité, déploiement |
| Partenaire paiement (Stripe) | Traitement des transactions |
| Autorités réglementaires | Conformité RGPD, accessibilité RGAA/WCAG 2.1 |

---

## Analyse des besoins utilisateurs

### Personas

#### Persona 1 — Client régulier (Maria, 34 ans, Barcelone)

- Utilise l'application depuis un smartphone en déplacement.
- Effectue plusieurs locations par an pour des voyages professionnels.
- Besoins : rapidité de réservation, historique accessible, modification facile.
- Frustrations actuelles : devoir ressaisir ses informations à chaque réservation.

#### Persona 2 — Client occasionnel (Thomas, 52 ans, Lyon)

- Peu à l'aise avec les interfaces numériques.
- Réserve 1 à 2 fois par an pour des vacances.
- Besoins : interface simple, messages d'erreur clairs, confirmation visible.
- Exigences d'accessibilité : taille de police ajustable, contraste suffisant.

#### Persona 3 — Utilisateur en situation de handicap (Amara, 29 ans, Londres)

- Malvoyante, utilise un lecteur d'écran (NVDA + Firefox).
- Besoins : navigation clavier complète, labels ARIA cohérents, ordre de focus logique.
- Exigences réglementaires : conformité WCAG 2.1 niveau AA, RGAA 4.1.

#### Persona 4 — Agent en agence (Kenji, 41 ans, Toronto)

- Accède à l'API pour consulter les réservations et mettre à jour les statuts.
- Besoins : API stable, documentation claire, réponses rapides.

### Parcours utilisateur principal — Réservation

```mermaid
flowchart LR
    A["Accueil"] --> B["Formulaire\nde recherche"]
    B --> C["Liste\ndes offres"]
    C --> D["Détail\nd'une offre"]
    D --> E["Saisie\ninformations"]
    E --> F["Récapitulatif"]
    F --> G["Paiement\nStripe"]
    G --> H["Confirmation"]
```

> Ce diagramme représente le parcours complet d'un client lors d'une réservation : depuis la page d'accueil, il remplit le formulaire de recherche, consulte la liste des offres disponibles, sélectionne une offre pour en voir le détail, saisit ses informations personnelles, vérifie le récapitulatif, procède au paiement via Stripe, puis reçoit la confirmation de sa réservation.

---

## Spécifications fonctionnelles

### SF-01 — Authentification et gestion de compte

| ID | Fonctionnalité | Priorité |
|---|---|---|
| SF-01-01 | Créer un compte (email + mot de passe) | Haute |
| SF-01-02 | Se connecter / déconnecter | Haute |
| SF-01-03 | Réinitialiser son mot de passe par email | Haute |
| SF-01-04 | Supprimer son compte (confirmation par mot de passe) | Haute |

**Règle métier** : La suppression de compte nécessite la saisie du mot de passe actuel. Les données personnelles sont anonymisées conformément au RGPD (droit à l'oubli). Les réservations futures actives bloquent la suppression jusqu'à leur annulation ou expiration.

### SF-02 — Gestion du profil

| ID | Fonctionnalité | Priorité |
|---|---|---|
| SF-02-01 | Consulter son profil | Haute |
| SF-02-02 | Modifier nom, prénom, date de naissance, adresse | Haute |
| SF-02-03 | Modifier son email (confirmation par lien) | Moyenne |
| SF-02-04 | Modifier son mot de passe | Haute |
| SF-02-05 | Gérer ses préférences de communication (emails marketing) | Basse |

### SF-03 — Recherche de véhicules

| ID | Fonctionnalité | Priorité |
|---|---|---|
| SF-03-01 | Afficher la liste des agences de location | Haute |
| SF-03-02 | Rechercher des offres via formulaire | Haute |
| SF-03-03 | Filtrer / trier les résultats | Moyenne |
| SF-03-04 | Consulter le détail d'une offre | Haute |

**Critères du formulaire de recherche** :
- Ville de départ (autocomplétion)
- Ville de retour (autocomplétion, peut différer)
- Date et heure de début
- Date et heure de retour
- Catégorie de véhicule (norme ACRISS)

**Règle métier** : La date de retour doit être postérieure d'au moins 2 heures à la date de départ.

**Catégories ACRISS** : L'application utilise la classification ACRISS standard (Mini, Économique, Compacte, Intermédiaire, Standard, Plein format, Premium, Luxe, SUV, Cabriolet, Monospace, etc.).

### SF-04 — Réservation

| ID | Fonctionnalité | Priorité |
|---|---|---|
| SF-04-01 | Réserver une offre de location | Haute |
| SF-04-02 | Pré-remplissage depuis le profil | Haute |
| SF-04-03 | Récapitulatif avant paiement | Haute |
| SF-04-04 | Paiement via Stripe | Haute |
| SF-04-05 | Confirmation par email | Haute |
| SF-04-06 | Consulter l'historique des réservations | Haute |
| SF-04-07 | Modifier une réservation | Haute |
| SF-04-08 | Annuler une réservation | Haute |

**Règles métier** :

- **Modification** : possible jusqu'à 48 heures avant le début de la réservation.
- **Annulation et remboursement** :
  - Plus de 7 jours avant le départ : remboursement intégral.
  - Moins de 7 jours avant le départ : remboursement de 25 % du montant total uniquement.
  - Moins de 48 heures avant le départ : aucun remboursement, modification impossible.
- **Paiement** : externalisé via Stripe. Aucune donnée de carte bancaire ne transite par les serveurs YCYW.

### SF-05 — API interne (usage agences)

| ID | Fonctionnalité | Priorité |
|---|---|---|
| SF-05-01 | CRUD utilisateurs | Haute |
| SF-05-02 | CRUD réservations | Haute |
| SF-05-03 | CRUD véhicules / offres | Haute |
| SF-05-04 | CRUD agences | Haute |
| SF-05-05 | Authentification par clé API ou JWT service | Haute |

### SF-06 — Tchat support en temps réel

| ID | Fonctionnalité | Priorité |
|---|---|---|
| SF-06-01 | Initier une session de tchat avec le support d'une agence | Haute |
| SF-06-02 | Envoyer et recevoir des messages en temps réel | Haute |
| SF-06-03 | Consulter l'historique des messages de la session en cours | Moyenne |
| SF-06-04 | Fermer une session de tchat | Moyenne |

**Règles métier** :

- Un client connecté peut ouvrir une session de tchat avec l'agence de son choix (depuis la page agence ou depuis une réservation active).
- Les messages sont transmis en temps réel via WebSocket (protocole STOMP) — aucun rechargement de page requis.
- L'historique de la conversation est persisté en base de données et rechargé à la réouverture de la session.
- Une session peut être fermée par le client ou l'agent ; elle passe alors au statut `closed`.
- La connexion WebSocket est authentifiée par JWT (le token est transmis à l'établissement de la connexion).

**Exigences d'accessibilité** :

- La fenêtre de tchat est navigable au clavier (focus piégé dans la fenêtre modale, Échap pour fermer).
- Les nouveaux messages entrants sont annoncés par un `role="log"` pour les lecteurs d'écran.
- Le champ de saisie est labellisé (`<label>` ou `aria-label`).

---

## Exigences non fonctionnelles

### Accessibilité

- Conformité **WCAG 2.1 niveau AA** (norme européenne EN 301 549).
- Conformité **RGAA 4.1** pour les marchés français.
- Navigation entièrement fonctionnelle au clavier.
- Compatibilité avec les lecteurs d'écran principaux : NVDA, JAWS, VoiceOver.
- Contraste minimum 4,5:1 pour les textes normaux, 3:1 pour les textes larges.
- Textes alternatifs sur toutes les images porteuses d'information.
- Messages d'erreur de formulaire explicites et visuellement associés au champ concerné.
- Pas de contenu animé ou clignotant susceptible de provoquer des crises.

### Sécurité

- Mots de passe hashés avec **Argon2id** (vainqueur Password Hashing Competition).
- **HTTPS obligatoire**, TLS 1.2 minimum (TLS 1.3 recommandé).
- Secrets stockés dans un **gestionnaire de secrets** (HashiCorp Vault ou équivalent cloud-native), jamais en dur dans le code.
- Rotation automatique des secrets d'accès aux services tiers.
- Protection contre OWASP Top 10 : injection SQL, XSS, CSRF, etc.
- Journalisation des accès et des actions sensibles (authentification, modification de réservation, suppression de compte).

### Performance

- Temps de réponse p95 < 500 ms pour les pages critiques (recherche, réservation).
- Disponibilité cible : **99,5 %** (vs 97,2 % actuel pour les apps FR/DE/ES/IT).
- Taux d'erreur lors des pics saisonniers < 0,5 % (vs jusqu'à 4 % actuellement).
- Charge supportée sans dégradation : ≥ 500 req/s (vs 150 req/s actuel FR).

### Internationalisation (i18n)

- Support multilingue : FR, EN, DE, ES, IT à minima.
- Gestion des fuseaux horaires (stockage UTC, affichage local).
- Formats de date/heure adaptés par locale.
- Conformité RGPD (UE) et loi 25 (Québec).

### Éco-conception

- Score Lighthouse Performance ≥ 85 sur desktop et mobile.
- Optimisation des assets (lazy loading, compression images WebP/AVIF).
- Pagination côté serveur pour éviter les surcharges réseau.
- Pas de requêtes inutiles côté client (cache HTTP, ETags).

---

## Contraintes et dépendances

| Contrainte | Détail |
|---|---|
| Stripe | Intégration obligatoire pour le paiement ; webhooks pour statuts de paiement |
| ACRISS | Classification des véhicules conforme à la norme |
| RGPD | Consentement explicite, droit d'accès et d'effacement |
| API agences | Rétrocompatibilité attendue lors du déploiement progressif |

