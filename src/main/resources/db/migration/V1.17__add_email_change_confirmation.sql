create table email_change_tokens (
    id bigserial not null,
    token_hash varchar(64) not null unique,
    pending_email varchar(255) not null unique,
    user_id uuid not null unique,
    expiration_date timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_email_change_tokens_user foreign key (user_id) references users (id)
);

alter table auth_email_deliveries drop constraint auth_email_deliveries_type_check;
alter table auth_email_deliveries add constraint auth_email_deliveries_type_check
    check (type in ('ACCOUNT_ACTIVATION', 'PASSWORD_RESET', 'EMAIL_CHANGE_CONFIRMATION'));
