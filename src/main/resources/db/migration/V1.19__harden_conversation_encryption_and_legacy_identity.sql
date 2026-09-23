alter table messages
    add column if not exists encryption_key_id varchar(100);

update messages
set encryption_key_id = 'default'
where encryption_key_id is null;

alter table messages
    alter column encryption_key_id set not null;

alter table messages
    add column if not exists sender_name_at_creation varchar(255);

update messages message
set sender_name_at_creation = coalesce(nullif(concat_ws(' ', app_user.first_name, app_user.last_name), ''), 'Deleted user')
from users app_user
where message.sender_id = app_user.id
  and message.sender_name_at_creation is null;

update messages
set sender_name_at_creation = 'Deleted user'
where sender_name_at_creation is null;

alter table messages
    alter column sender_name_at_creation set not null;

alter table conversation_participant
    add column if not exists user_name_at_join varchar(255);

update conversation_participant participant
set user_name_at_join = coalesce(nullif(concat_ws(' ', app_user.first_name, app_user.last_name), ''), 'Deleted user')
from users app_user
where participant.user_id = app_user.id
  and participant.user_name_at_join is null;

update conversation_participant
set user_name_at_join = 'Deleted user'
where user_name_at_join is null;

alter table conversation_participant
    alter column user_name_at_join set not null;

alter table messages
    drop constraint if exists "FK4ui4nnwntodh6wjvck53dbk9m";

alter table messages
    add constraint fk_messages_sender
    foreign key (sender_id) references users on delete set null;

alter table conversation_participant
    alter column user_id drop not null;

alter table conversation_participant
    drop constraint if exists fk_conversation_participant_user;

alter table conversation_participant
    add constraint fk_conversation_participant_user
    foreign key (user_id) references users on delete set null;
