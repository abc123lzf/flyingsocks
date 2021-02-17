create table access_rule
(
    id          int auto_increment
        primary key,
    pattern     varchar(255) not null,
    type        int          not null,
    create_time timestamp    null,
    enable      tinyint      null
);

create table traffic_rule
(
    id            int auto_increment
        primary key,
    daily_limit   bigint                              null,
    weekly_limit  bigint                              null,
    monthly_limit bigint                              null,
    speed_limit   int                                 null,
    create_time   timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    enable        tinyint                             not null
);

create table traffic_usage_log
(
    id                     bigint auto_increment
        primary key,
    user_id                int                                 not null,
    upload_traffic_usage   bigint                              null,
    download_traffic_usage bigint                              not null,
    type                   enum ('daily', 'weekly', 'monthly') not null,
    date                   char(8)                             not null,
    update_time            timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create table user
(
    id          int auto_increment
        primary key,
    username    varchar(255)                        not null,
    password    char(32)                            not null,
    group_id    int                                 null,
    create_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    status      int                                 not null,
    constraint user_username_uindex
        unique (username)
);

create table user_access_log
(
    id                     bigint auto_increment
        primary key,
    user_id                int          not null,
    address                varchar(255) not null,
    upload_traffic_usage   bigint       not null,
    download_traffic_usage bigint       not null,
    connect_time           timestamp    null,
    disconnect_time        timestamp    null
);

create table user_group
(
    id          int auto_increment
        primary key,
    name        varchar(255)                        not null,
    parent_id   int                                 null,
    attributes  varchar(4096)                       null,
    create_time timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create table user_group_access_rule
(
    id             int auto_increment
        primary key,
    group_id       int                                 not null,
    access_rule_id int                                 not null,
    create_time    timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create table user_group_traffic_rule
(
    id              int auto_increment
        primary key,
    group_id        int                                 not null,
    traffic_rule_id int                                 null,
    create_time     timestamp default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);
