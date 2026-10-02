CREATE TABLE IF NOT EXISTS user_event_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_user_event_interactions PRIMARY KEY (user_id, event_id)
);

CREATE INDEX IF NOT EXISTS idx_user_event_interactions_user_id
    ON user_event_interactions (user_id);

CREATE INDEX IF NOT EXISTS idx_user_event_interactions_event_id
    ON user_event_interactions (event_id);

CREATE TABLE IF NOT EXISTS event_similarity (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_event_similarity PRIMARY KEY (event_a, event_b),
    CONSTRAINT chk_event_similarity_order CHECK (event_a < event_b)
);

CREATE INDEX IF NOT EXISTS idx_event_similarity_event_a
    ON event_similarity (event_a);

CREATE INDEX IF NOT EXISTS idx_event_similarity_event_b
    ON event_similarity (event_b);
