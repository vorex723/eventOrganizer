alter table notification_devices
    drop constraint chk_notification_devices_platform;

alter table notification_devices
    add constraint chk_notification_devices_platform
        check (platform in ('ANDROID', 'IOS', 'WEB'));
