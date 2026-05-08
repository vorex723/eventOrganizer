delete from messages
where conversation_id is null;

create sequence if not exists message_id_seq
    start with 1
    increment by 50;

alter sequence message_id_seq increment by 50;

alter table messages
    add column new_id bigint;

update messages
set new_id = nextval('message_id_seq')
where new_id is null;

alter table messages
    alter column new_id set not null;

alter table messages
    drop constraint if exists messages_pkey;

alter table messages
    drop column id;

alter table messages
    rename column new_id to id;

alter table messages
    add primary key (id);

alter table messages
    rename column message to content;

alter table messages
    alter column content type varchar(3000);

update messages
set content = ''
where content is null;

alter table messages
    alter column content set not null;

alter table messages
    alter column sent_date type timestamp(6) with time zone
    using sent_date at time zone 'UTC';

update messages
set sent_date = now()
where sent_date is null;

alter table messages
    alter column sent_date set not null;

alter table messages
    alter column conversation_id set not null;

alter table conversations
    add column type varchar(255);

update conversations c
set type = case
    when (
        select count(*)
        from user_conversation uc
        where uc.conversation_id = c.id
    ) > 2 then 'GROUP'
    else 'DIRECT'
end
where type is null;

alter table conversations
    alter column type set not null;

alter table conversations
    add constraint chk_conversations_type
    check (type in ('DIRECT', 'GROUP'));

alter table conversations
    add column created_at timestamp(6) with time zone;

alter table conversations
    add column last_active_at timestamp(6) with time zone;

update conversations c
set created_at = coalesce(
    (
        select min(m.sent_date)
        from messages m
        where m.conversation_id = c.id
    ),
    now()
)
where created_at is null;

update conversations c
set last_active_at = coalesce(
    (
        select max(m.sent_date)
        from messages m
        where m.conversation_id = c.id
    ),
    c.created_at,
    now()
)
where last_active_at is null;

alter table conversations
    alter column created_at set not null;

alter table conversations
    alter column last_active_at set not null;

create table conversation_participant (
    id bigserial not null,
    conversation_id uuid not null,
    user_id uuid not null,
    joined_at timestamp(6) with time zone not null,
    left_at timestamp(6) with time zone,
    last_read_at timestamp(6) with time zone,
    last_read_message_id bigint,
    primary key (id),
    constraint uk_conversation_participant_conversation_user unique (conversation_id, user_id)
);

alter table conversation_participant
    add constraint fk_conversation_participant_conversation
    foreign key (conversation_id) references conversations;

alter table conversation_participant
    add constraint fk_conversation_participant_user
    foreign key (user_id) references users;

insert into conversation_participant (conversation_id, user_id, joined_at)
select uc.conversation_id, uc.user_id, c.created_at
from user_conversation uc
join conversations c on c.id = uc.conversation_id
on conflict (conversation_id, user_id) do nothing;

create index idx_message_conversation_created
    on messages (conversation_id, sent_date desc);

create index idx_message_conversation_id
    on messages (conversation_id, id);

create index idx_conversation_participant_user
    on conversation_participant (user_id);

create index idx_conversation_participant_conversation
    on conversation_participant (conversation_id);
