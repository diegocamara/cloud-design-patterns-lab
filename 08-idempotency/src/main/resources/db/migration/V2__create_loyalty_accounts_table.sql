CREATE TABLE loyalty_accounts
(
    customer_id UUID PRIMARY KEY,
    points      BIGINT      NOT NULL DEFAULT 0,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_loyalty_accounts_points
        CHECK (points >= 0)
);