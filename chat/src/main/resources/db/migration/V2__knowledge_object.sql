CREATE TABLE IF NOT EXISTS knowledge_object (
    uri          TEXT PRIMARY KEY,
    fingerprint  TEXT NOT NULL,
    modality     TEXT NOT NULL,
    indexed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
