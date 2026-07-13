create table players
(
    id         uuid primary key,
    nickname   varchar(100)             not null,
    experience integer                  not null,
    level      integer                  not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table completed_stages
(
    player_id    uuid                     not null,
    stage_code   varchar(100)             not null,
    xp_gained    integer                  not null,
    completed_at timestamp with time zone not null,

    primary key (player_id, stage_code),

    constraint fk_completed_stages_player
        foreign key (player_id)
            references players (id)
);