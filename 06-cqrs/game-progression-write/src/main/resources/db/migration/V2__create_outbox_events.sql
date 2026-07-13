create table outbox_events
(
    id             uuid primary key,
    aggregate_id   uuid                     not null,
    aggregate_type varchar(100)             not null,
    event_type     varchar(100)             not null,
    payload        jsonb                    not null,
    status         varchar(50)              not null,
    attempts       integer                  not null default 0,
    last_error     text null,
    created_at     timestamp with time zone not null,
    published_at   timestamp with time zone null
);

create index idx_outbox_events_status_created_at
    on outbox_events (status, created_at);

create index idx_outbox_events_aggregate_id
    on outbox_events (aggregate_id);