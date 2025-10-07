DROP TABLE IF EXISTS webhooks_outbox;
DROP TYPE IF EXISTS delivery_status;

CREATE TYPE delivery_status AS ENUM ('pending', 'delivering', 'delivered', 'dead');

CREATE TABLE IF NOT EXISTS webhooks_outbox (
    id UUID PRIMARY KEY,
    aggregate_id TEXT NOT NULL,
    seq INT NOT NULL,
    target_url TEXT NOT NULL,
    payload JSONB NOT NULL,
    status delivery_status NOT NULL DEFAULT 'pending',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NULL,
    http_code INT NULL,
    last_error TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (aggregate_id, seq)
);

CREATE INDEX IF NOT EXISTS idx_status_next_attempt_at
    ON webhooks_outbox (status, next_attempt_at);

CREATE INDEX IF NOT EXISTS idx_aggregate_seq
    ON webhooks_outbox (aggregate_id, seq);
