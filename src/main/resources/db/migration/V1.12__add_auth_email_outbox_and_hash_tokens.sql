create extension if not exists pgcrypto;

alter table activation_tokens add column token_hash varchar(64);
update activation_tokens set token_hash = encode(digest(token::text, 'sha256'), 'hex');
alter table activation_tokens alter column token_hash set not null;
alter table activation_tokens add constraint uk_activation_tokens_token_hash unique (token_hash);
alter table activation_tokens drop column token;

create table auth_email_deliveries (
    id uuid not null,
    user_id uuid not null,
    recipient_email varchar(255) not null,
    type varchar(32) not null check (type in ('ACCOUNT_ACTIVATION', 'PASSWORD_RESET')),
    encrypted_token varchar(4096) not null,
    status varchar(16) not null check (status in ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD', 'CANCELLED')),
    attempt_count integer not null,
    next_attempt_at timestamp(6) with time zone,
    processing_started_at timestamp(6) with time zone,
    claim_token uuid,
    sent_at timestamp(6) with time zone,
    last_error varchar(3000),
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_auth_email_deliveries_user foreign key (user_id) references users (id)
);

create index idx_auth_email_deliveries_processable
    on auth_email_deliveries (status, next_attempt_at, processing_started_at, created_at)
    where status in ('PENDING', 'FAILED', 'PROCESSING');

create index idx_auth_email_deliveries_user_type_created
    on auth_email_deliveries (user_id, type, created_at desc);
