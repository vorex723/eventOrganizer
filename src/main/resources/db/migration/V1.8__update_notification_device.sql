-- Firebase Installation IDs cannot be derived from legacy FCM push tokens.
-- Remove existing registrations so clients can register their installation again.
delete from notification_devices;

drop index idx_notification_devices_user_platform_active;

alter table notification_devices
    drop constraint uq_notification_devices_push_token;

alter table notification_devices
    drop column active,
    drop column push_token;

alter table notification_devices
    add column firebase_installation_id varchar(255) not null,
    add constraint uq_notification_devices_firebase_installation_id
        unique (firebase_installation_id);

create index idx_notification_devices_user_platform
    on notification_devices (user_id, platform);
