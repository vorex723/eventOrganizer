alter table notification_deliveries
    drop constraint if exists uq_notification_deliveries_notification_channel;

alter table notification_deliveries
    add column target_key varchar(320),
    add column target_email varchar(320),
    add column target_device_id uuid,
    add column target_installation_id varchar(255);

-- Existing pending rows did not retain a destination.  Do not deliver them to a
-- potentially changed account/device after this migration.
update notification_deliveries
set status = 'SKIPPED',
    last_error = 'Delivery target unavailable after target snapshot migration.',
    next_attempt_at = null,
    processing_started_at = null,
    claim_token = null
where status in ('PENDING', 'FAILED', 'PROCESSING');

update notification_deliveries
set target_key = 'legacy:' || id::text
where target_key is null;

alter table notification_deliveries
    alter column target_key set not null;

alter table notification_deliveries
    add constraint uq_notification_deliveries_target
        unique (notification_id, channel, target_key);

create index idx_notification_deliveries_target_device
    on notification_deliveries (target_device_id)
    where target_device_id is not null;
