create table direct_conversation_pair (
    id bigserial not null,
    conversation_id uuid not null,
    first_user_id uuid not null,
    second_user_id uuid not null,
    primary key (id),
    constraint uq_direct_conversation_pair_conversation unique (conversation_id),
    constraint uq_direct_conversation_pair_users unique (first_user_id, second_user_id),
    constraint chk_direct_conversation_pair_user_order check (first_user_id < second_user_id)
);

alter table direct_conversation_pair
    add constraint fk_direct_conversation_pair_conversation
    foreign key (conversation_id) references conversations on delete cascade;

alter table direct_conversation_pair
    add constraint fk_direct_conversation_pair_first_user
    foreign key (first_user_id) references users;

alter table direct_conversation_pair
    add constraint fk_direct_conversation_pair_second_user
    foreign key (second_user_id) references users;

insert into direct_conversation_pair (conversation_id, first_user_id, second_user_id)
select
    c.id,
    case
        when first_participant.user_id < second_participant.user_id then first_participant.user_id
        else second_participant.user_id
    end,
    case
        when first_participant.user_id < second_participant.user_id then second_participant.user_id
        else first_participant.user_id
    end
from conversations c
join conversation_participant first_participant
    on first_participant.conversation_id = c.id
join conversation_participant second_participant
    on second_participant.conversation_id = c.id
    and first_participant.user_id < second_participant.user_id
where c.type = 'DIRECT'
  and (
      select count(participant)
      from conversation_participant participant
      where participant.conversation_id = c.id
  ) = 2
on conflict (first_user_id, second_user_id) do nothing;
