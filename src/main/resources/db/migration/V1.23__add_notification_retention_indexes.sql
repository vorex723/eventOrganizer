create index idx_notification_deliveries_dead_created_at
    on notification_deliveries (created_at)
    where status = 'DEAD';

create index idx_notifications_created_at
    on notifications (created_at);
