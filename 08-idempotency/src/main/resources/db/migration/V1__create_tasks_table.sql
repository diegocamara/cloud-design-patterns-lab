CREATE TABLE tasks
(
    id         UUID PRIMARY KEY,
    title      VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);