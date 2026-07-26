CREATE TABLE processed_messages
(
    consumer_name   VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_processed_messages
        PRIMARY KEY (
                     consumer_name,
                     idempotency_key
            )
);