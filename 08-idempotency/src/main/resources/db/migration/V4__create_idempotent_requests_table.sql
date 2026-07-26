CREATE TABLE idempotent_requests
(
    id              UUID PRIMARY KEY,
    operation_name  VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,

    status          VARCHAR(20)  NOT NULL,
    http_status     INTEGER,
    response_body   JSONB,

    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uk_idempotent_requests
        UNIQUE (
                operation_name,
                idempotency_key
            ),

    CONSTRAINT ck_idempotent_requests_status
        CHECK (
            status IN ('PROCESSING', 'COMPLETED')
            ),

    CONSTRAINT ck_idempotent_requests_completed
        CHECK (
            status <> 'COMPLETED'
                OR (
                http_status IS NOT NULL
                    AND completed_at IS NOT NULL
                )
            )
);

CREATE INDEX idx_idempotent_requests_expires_at
    ON idempotent_requests (expires_at);