alter table notification_deliveries
    drop constraint chk_notification_deliveries_channel;

alter table notification_preferences
    drop constraint chk_notification_preferences_channel;

update notification_deliveries
set channel = 'PUSH_MOBILE'
where channel = 'PUSH_ANDROID';

update notification_preferences
set channel = 'PUSH_MOBILE'
where channel = 'PUSH_ANDROID';

alter table notification_deliveries
    add constraint chk_notification_deliveries_channel
        check (channel in ('PUSH_MOBILE', 'PUSH_WEB', 'EMAIL'));

alter table notification_preferences
    add constraint chk_notification_preferences_channel
        check (channel in ('PUSH_MOBILE', 'PUSH_WEB', 'EMAIL'));
