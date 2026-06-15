CREATE TABLE orders
(
    id           UUID           NOT NULL,
    customer_id  UUID           NOT NULL,
    status       VARCHAR(30)    NOT NULL,
    total_amount NUMERIC(19, 2) NOT NULL,
    created_at   TIMESTAMP      NOT NULL,
    updated_at   TIMESTAMP      NOT NULL,
    constraint id_pk primary key (id)
);