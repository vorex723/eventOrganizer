alter table notification_deliveries
    add column processing_started_at timestamp(6) with time zone,
    add column claim_token uuid;

drop index idx_notification_deliveries_processable;

create index idx_notification_deliveries_processable
    on notification_deliveries (status, next_attempt_at, processing_started_at, created_at)
    where status in ('PENDING', 'FAILED', 'PROCESSING');

create index idx_notification_deliveries_claim_token
    on notification_deliveries (claim_token)
    where claim_token is not null;
