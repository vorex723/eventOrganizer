delete from refresh_tokens;

alter table users
    add column security_version bigint not null default 0;

alter table refresh_tokens
    drop column token,
    add column token_hash varchar(64) not null unique,
    add column family_id uuid not null;

create index idx_refresh_tokens_family_id on refresh_tokens (family_id);
