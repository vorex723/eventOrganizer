alter table threads add column if not exists last_activity timestamp(6) with time zone;

alter table threads rename column edit_counter to edit_count;

alter table threads add column if not exists reply_count integer not null default 0;

update threads set last_activity = create_date where last_activity = null;

alter table threads alter column edit_count set default 0;

update threads set edit_count = 0 where edit_count is null;

alter table threads alter column edit_count set not null;
