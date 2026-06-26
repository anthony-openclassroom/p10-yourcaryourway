-- Extension pgcrypto pour gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Sessions de chat entre un client et une agence
CREATE TABLE IF NOT EXISTS chat_sessions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,                          -- ID du client (géré par le service auth externe)
    agency_id   UUID        NOT NULL,                          -- ID de l'agence (géré par le service auth externe)
    status      VARCHAR(10) NOT NULL DEFAULT 'open'
                CONSTRAINT chat_sessions_status_check CHECK (status IN ('open', 'closed')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at   TIMESTAMPTZ                                    -- NULL tant que la session est ouverte
);

-- Messages échangés au sein d'une session
CREATE TABLE IF NOT EXISTS chat_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id  UUID        NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    sender_role VARCHAR(10) NOT NULL
                CONSTRAINT chat_messages_role_check CHECK (sender_role IN ('client', 'agent')),
    content     TEXT        NOT NULL,
    sent_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index pour accélérer la récupération des messages d'une session
CREATE INDEX IF NOT EXISTS idx_chat_messages_session_id ON chat_messages(session_id);
-- Index pour accélérer la recherche des sessions d'un utilisateur
CREATE INDEX IF NOT EXISTS idx_chat_sessions_user_id    ON chat_sessions(user_id);
