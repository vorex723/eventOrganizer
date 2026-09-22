create table password_reset_tokens (
    id bigserial not null,
    token_hash varchar(64) not null unique,
    user_id uuid not null unique,
    expiration_date timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_password_reset_tokens_user foreign key (user_id) references users (id)
);
