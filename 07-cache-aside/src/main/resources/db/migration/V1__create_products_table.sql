CREATE TABLE products
(
    id    UUID PRIMARY KEY,
    name  VARCHAR(255)   NOT NULL,
    price NUMERIC(19, 2) NOT NULL
);