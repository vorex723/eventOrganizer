alter table notifications
    drop constraint if exists FK9y21adhxn0ayjhfocscqox7bh;

alter table notifications
    drop constraint if exists notifications_type_check;

alter table notifications
    rename column user_id to recipient_id;

alter table notifications
    rename column create_date to created_at;

alter table notifications
    rename column type to resource_type;

alter table notifications
    add column parent_resource_type varchar(255),
    add column parent_resource_id uuid,
    add column read_at timestamp(6) with time zone;

alter table notifications
    alter column created_at type timestamp(6) with time zone
    using created_at at time zone 'UTC';

update notifications
set created_at = now()
where created_at is null;

update notifications
set read_at = created_at
where opened is true;

update notifications
set resource_type = case resource_type
    when 'EVENT_UPDATE' then 'EVENT'
    when 'EVENT_NEW_FILE' then 'FILE'
    when 'EVENT_NEW_THREAD' then 'THREAD'
    when 'THREAD_REPLY' then 'THREAD'
    when 'PRIVATE_MESSAGE' then 'CONVERSATION'
    else resource_type
end;

update notifications notification
set parent_resource_type = 'EVENT',
    parent_resource_id = file.event_id
from files file
where notification.resource_type = 'FILE'
  and notification.resource_id = file.id
  and file.event_id is not null;

update notifications notification
set parent_resource_type = 'EVENT',
    parent_resource_id = thread.event_id
from threads thread
where notification.resource_type = 'THREAD'
  and notification.resource_id = thread.id
  and thread.event_id is not null;

update notifications
set title = ''
where title is null;

update notifications
set body = ''
where body is null;

alter table notifications
    alter column recipient_id set not null,
    alter column title set not null,
    alter column body type varchar(3000),
    alter column body set not null,
    alter column resource_type set not null,
    alter column resource_id set not null,
    alter column created_at set not null;

alter table notifications
    drop column opened;

alter table notifications
    add constraint fk_notifications_recipient
        foreign key (recipient_id) references users,
    add constraint chk_notifications_resource_type
        check (resource_type in ('EVENT', 'THREAD', 'FILE', 'CONVERSATION', 'USER')),
    add constraint chk_notifications_parent_resource_type
        check (parent_resource_type is null or parent_resource_type in ('EVENT', 'THREAD', 'FILE', 'CONVERSATION', 'USER')),
    add constraint chk_notifications_parent_reference
        check (
            (parent_resource_type is null and parent_resource_id is null)
            or
            (parent_resource_type is not null and parent_resource_id is not null)
        );

create index idx_notifications_recipient_created_at
    on notifications (recipient_id, created_at desc);

create index idx_notifications_recipient_unread
    on notifications (recipient_id)
    where read_at is null;

create table notification_deliveries (
    id uuid not null,
    notification_id uuid not null,
    channel varchar(255) not null,
    status varchar(255) not null,
    attempt_count integer not null,
    next_attempt_at timestamp(6) with time zone,
    sent_at timestamp(6) with time zone,
    provider_message_id varchar(255),
    last_error varchar(3000),
    created_at timestamp(6) with time zone not null,
    primary key (id),
    constraint uq_notification_deliveries_notification_channel unique (notification_id, channel),
    constraint chk_notification_deliveries_channel
        check (channel in ('IN_APP', 'PUSH_ANDROID', 'EMAIL')),
    constraint chk_notification_deliveries_status
        check (status in ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD', 'SKIPPED')),
    constraint chk_notification_deliveries_attempt_count
        check (attempt_count >= 0),
    constraint fk_notification_deliveries_notification
        foreign key (notification_id) references notifications on delete cascade
);

create index idx_notification_deliveries_processable
    on notification_deliveries (status, next_attempt_at)
    where status in ('PENDING', 'FAILED');

create table notification_devices (
    id uuid not null,
    user_id uuid not null,
    platform varchar(255) not null,
    push_token varchar(1000) not null,
    active boolean not null,
    created_at timestamp(6) with time zone not null,
    last_seen_at timestamp(6) with time zone,
    primary key (id),
    constraint uq_notification_devices_push_token unique (push_token),
    constraint chk_notification_devices_platform
        check (platform in ('ANDROID')),
    constraint fk_notification_devices_user
        foreign key (user_id) references users on delete cascade
);

insert into notification_devices (
    id,
    user_id,
    platform,
    push_token,
    active,
    created_at,
    last_seen_at
)
select
    md5(user_id::text || ':ANDROID')::uuid,
    user_id,
    'ANDROID',
    push_token,
    true,
    created_at,
    created_at
from (
    select distinct on (fcm_android_token)
        id as user_id,
        fcm_android_token as push_token,
        created_at
    from users
    where nullif(btrim(fcm_android_token), '') is not null
    order by fcm_android_token, created_at desc, id
) legacy_devices;

alter table users
    drop column fcm_android_token;

create index idx_notification_devices_user_platform_active
    on notification_devices (user_id, platform, active);

create table notification_preferences (
    id uuid not null,
    user_id uuid not null,
    resource_type varchar(255) not null,
    channel varchar(255) not null,
    enabled boolean not null,
    primary key (id),
    constraint uq_notification_preferences_user_resource_channel
        unique (user_id, resource_type, channel),
    constraint chk_notification_preferences_resource_type
        check (resource_type in ('EVENT', 'THREAD', 'FILE', 'CONVERSATION', 'USER')),
    constraint chk_notification_preferences_channel
        check (channel in ('IN_APP', 'PUSH_ANDROID', 'EMAIL')),
    constraint fk_notification_preferences_user
        foreign key (user_id) references users on delete cascade
);

create index idx_notification_preferences_user
    on notification_preferences (user_id);
