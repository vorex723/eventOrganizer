alter table users
    add column notification_preferences_version bigint not null default 0;
