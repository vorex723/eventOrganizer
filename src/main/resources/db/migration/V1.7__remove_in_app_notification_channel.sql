delete from notification_deliveries
where channel = 'IN_APP';

delete from notification_preferences
where channel = 'IN_APP';

alter table notification_deliveries
    drop constraint chk_notification_deliveries_channel;

alter table notification_deliveries
    add constraint chk_notification_deliveries_channel
        check (channel in ('PUSH_ANDROID', 'EMAIL'));

alter table notification_preferences
    drop constraint chk_notification_preferences_channel;

alter table notification_preferences
    add constraint chk_notification_preferences_channel
        check (channel in ('PUSH_ANDROID', 'EMAIL'));
