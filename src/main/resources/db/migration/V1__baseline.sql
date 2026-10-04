-- V1 baseline for a clean database before the first deployment.
-- Legacy data compatibility is intentionally unsupported before the first deployment.
-- UUIDs and timestamps are supplied by the application. No sample accounts or provider calls.

CREATE TABLE cities (
    id uuid PRIMARY KEY,
    external_id varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    country_code varchar(2) NOT NULL,
    admin_area varchar(255),
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    time_zone_id varchar(255) NOT NULL,
    CONSTRAINT uk_city_external_id UNIQUE (external_id),
    CONSTRAINT chk_city_external_id CHECK (external_id ~ '[^[:space:]]'),
    CONSTRAINT chk_city_name CHECK (name ~ '[^[:space:]]'),
    CONSTRAINT chk_city_country_code CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_city_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_city_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_city_time_zone_id CHECK (time_zone_id ~ '[^[:space:]]')
);

CREATE TABLE roles (
    id bigserial PRIMARY KEY,
    name varchar(255) NOT NULL UNIQUE CHECK (name ~ '[^[:space:]]')
);

INSERT INTO roles (name) VALUES ('ROLE_USER'), ('ROLE_ADMIN'), ('ROLE_MODERATOR');

CREATE TABLE users (
    id uuid PRIMARY KEY,
    first_name varchar(255) NOT NULL CHECK (first_name ~ '[^[:space:]]'),
    last_name varchar(255) NOT NULL CHECK (last_name ~ '[^[:space:]]'),
    email varchar(255) NOT NULL CHECK (email ~ '[^[:space:]]'),
    password varchar(255) NOT NULL CHECK (password ~ '[^[:space:]]'),
    city_id uuid NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    time_zone varchar(255) NOT NULL CHECK (time_zone ~ '[^[:space:]]'),
    last_credentials_change_time timestamp(6) with time zone NOT NULL,
    security_version bigint NOT NULL DEFAULT 0 CHECK (security_version >= 0),
    notification_preferences_version bigint NOT NULL DEFAULT 0 CHECK (notification_preferences_version >= 0),
    activated boolean NOT NULL DEFAULT false,
    banned boolean NOT NULL DEFAULT false,
    CONSTRAINT users_email_key UNIQUE (email),
    CONSTRAINT fk_users_city FOREIGN KEY (city_id) REFERENCES cities
);

CREATE TABLE user_roles (
    user_id uuid NOT NULL,
    role_id bigint NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles
);

CREATE TABLE tags (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL CHECK (name ~ '[^[:space:]]')
);

-- This expression must match TagRepository.insertIfAbsent's ON CONFLICT target.
CREATE UNIQUE INDEX uq_tags_normalized_name ON tags ((lower(btrim(name))));

CREATE TABLE events (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL CHECK (name ~ '[^[:space:]]'),
    short_description varchar(255) NOT NULL CHECK (short_description ~ '[^[:space:]]'),
    long_description text NOT NULL CHECK (long_description ~ '[^[:space:]]'),
    create_date timestamp(6) with time zone NOT NULL,
    last_update timestamp(6) with time zone NOT NULL,
    event_start_date timestamp(6) with time zone NOT NULL,
    city_id uuid NOT NULL,
    exact_address varchar(255) NOT NULL CHECK (exact_address ~ '[^[:space:]]'),
    user_id uuid,
    max_attendees integer,
    attendee_count integer NOT NULL DEFAULT 0 CHECK (attendee_count >= 0),
    CONSTRAINT chk_events_max_attendees CHECK (max_attendees IS NULL OR max_attendees >= 1),
    CONSTRAINT fk_events_city FOREIGN KEY (city_id) REFERENCES cities,
    CONSTRAINT fk_events_owner FOREIGN KEY (user_id) REFERENCES users ON DELETE SET NULL
);

CREATE TABLE event_user (
    event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_event_user_event FOREIGN KEY (event_id) REFERENCES events,
    CONSTRAINT fk_event_user_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE event_tag (
    event_id uuid NOT NULL,
    tag_id uuid NOT NULL,
    PRIMARY KEY (event_id, tag_id),
    CONSTRAINT fk_event_tag_event FOREIGN KEY (event_id) REFERENCES events,
    CONSTRAINT fk_event_tag_tag FOREIGN KEY (tag_id) REFERENCES tags
);

CREATE TABLE threads (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL,
    user_id uuid,
    name varchar(255) NOT NULL CHECK (name ~ '[^[:space:]]'),
    content varchar(1000) NOT NULL CHECK (content ~ '[^[:space:]]'),
    create_date timestamp(6) with time zone NOT NULL,
    last_update timestamp(6) with time zone NOT NULL,
    last_activity timestamp(6) with time zone NOT NULL,
    edit_count integer NOT NULL DEFAULT 0 CHECK (edit_count >= 0),
    reply_count integer NOT NULL DEFAULT 0 CHECK (reply_count >= 0),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT fk_threads_event FOREIGN KEY (event_id) REFERENCES events,
    CONSTRAINT fk_threads_owner FOREIGN KEY (user_id) REFERENCES users ON DELETE SET NULL
);

CREATE TABLE thread_replies (
    id uuid PRIMARY KEY,
    thread_id uuid NOT NULL,
    user_id uuid,
    content varchar(1000) NOT NULL CHECK (content ~ '[^[:space:]]'),
    reply_date timestamp(6) with time zone NOT NULL,
    last_update timestamp(6) with time zone NOT NULL,
    edit_count integer NOT NULL DEFAULT 0 CHECK (edit_count >= 0),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT fk_thread_replies_thread FOREIGN KEY (thread_id) REFERENCES threads,
    CONSTRAINT fk_thread_replies_replier FOREIGN KEY (user_id) REFERENCES users ON DELETE SET NULL
);

CREATE TABLE files (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL,
    user_id uuid,
    user_file_name varchar(255) NOT NULL CHECK (user_file_name ~ '[^[:space:]]'),
    original_file_name varchar(255) NOT NULL CHECK (original_file_name ~ '[^[:space:]]'),
    content_type varchar(255) NOT NULL CHECK (content_type ~ '[^[:space:]]'),
    content bytea NOT NULL,
    upload_date_time timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_files_event FOREIGN KEY (event_id) REFERENCES events,
    CONSTRAINT fk_files_owner FOREIGN KEY (user_id) REFERENCES users ON DELETE SET NULL
);

CREATE TABLE conversations (
    id uuid PRIMARY KEY,
    type varchar(255) NOT NULL CHECK (type IN ('DIRECT', 'GROUP')),
    name varchar(255),
    created_at timestamp(6) with time zone NOT NULL,
    last_active_at timestamp(6) with time zone NOT NULL
);

CREATE TABLE conversation_participant (
    id bigserial PRIMARY KEY,
    conversation_id uuid NOT NULL,
    user_id uuid,
    user_name_at_join varchar(255) NOT NULL CHECK (user_name_at_join ~ '[^[:space:]]'),
    joined_at timestamp(6) with time zone NOT NULL,
    left_at timestamp(6) with time zone,
    last_read_at timestamp(6) with time zone,
    last_read_message_id bigint,
    CONSTRAINT uk_conversation_participant_conversation_user UNIQUE (conversation_id, user_id),
    CONSTRAINT fk_conversation_participant_conversation FOREIGN KEY (conversation_id) REFERENCES conversations,
    CONSTRAINT fk_conversation_participant_user FOREIGN KEY (user_id) REFERENCES users ON DELETE SET NULL
);

CREATE TABLE direct_conversation_pair (
    id bigserial PRIMARY KEY,
    conversation_id uuid NOT NULL,
    first_user_id uuid NOT NULL,
    second_user_id uuid NOT NULL,
    CONSTRAINT uq_direct_conversation_pair_conversation UNIQUE (conversation_id),
    CONSTRAINT uq_direct_conversation_pair_users UNIQUE (first_user_id, second_user_id),
    CONSTRAINT chk_direct_conversation_pair_user_order CHECK (first_user_id < second_user_id),
    CONSTRAINT fk_direct_conversation_pair_conversation FOREIGN KEY (conversation_id) REFERENCES conversations ON DELETE CASCADE,
    CONSTRAINT fk_direct_conversation_pair_first_user FOREIGN KEY (first_user_id) REFERENCES users ON DELETE CASCADE,
    CONSTRAINT fk_direct_conversation_pair_second_user FOREIGN KEY (second_user_id) REFERENCES users ON DELETE CASCADE
);

CREATE SEQUENCE message_id_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE messages (
    id bigint PRIMARY KEY,
    conversation_id uuid NOT NULL,
    sent_date timestamp(6) with time zone NOT NULL,
    sender_id uuid,
    sender_name_at_creation varchar(255) NOT NULL CHECK (sender_name_at_creation ~ '[^[:space:]]'),
    encryption_key_id varchar(100) NOT NULL CHECK (encryption_key_id ~ '[^[:space:]]'),
    content text NOT NULL CHECK (content ~ '[^[:space:]]'),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users ON DELETE SET NULL
);

CREATE TABLE activation_tokens (
    id bigserial PRIMARY KEY,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '[^[:space:]]'),
    user_id uuid NOT NULL UNIQUE,
    expiration_date timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_activation_tokens_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
    id bigserial PRIMARY KEY,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '[^[:space:]]'),
    user_id uuid NOT NULL UNIQUE,
    expiration_date timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE email_change_tokens (
    id bigserial PRIMARY KEY,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '[^[:space:]]'),
    pending_email varchar(255) NOT NULL CHECK (pending_email ~ '[^[:space:]]'),
    user_id uuid NOT NULL UNIQUE,
    expiration_date timestamp(6) with time zone NOT NULL,
    CONSTRAINT email_change_tokens_pending_email_key UNIQUE (pending_email),
    CONSTRAINT fk_email_change_tokens_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id bigserial PRIMARY KEY,
    token_hash varchar(64) NOT NULL UNIQUE CHECK (token_hash ~ '[^[:space:]]'),
    family_id uuid NOT NULL,
    user_id uuid NOT NULL,
    expiry_date timestamp(6) with time zone NOT NULL,
    revoked boolean NOT NULL DEFAULT false,
    device_type varchar(255) NOT NULL CHECK (device_type IN (
        'MOBILE_ANDROID', 'MOBILE_IOS', 'MOBILE_OTHER', 'TABLET_ANDROID', 'TABLET_IOS',
        'TABLET_OTHER', 'DESKTOP', 'WEB', 'UNKNOWN')),
    created_at timestamp(6) with time zone NOT NULL,
    last_used_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE auth_email_deliveries (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    recipient_email varchar(255) NOT NULL CHECK (recipient_email ~ '[^[:space:]]'),
    type varchar(32) NOT NULL CHECK (type IN ('ACCOUNT_ACTIVATION', 'PASSWORD_RESET', 'EMAIL_CHANGE_CONFIRMATION')),
    encrypted_token varchar(4096) NOT NULL CHECK (encrypted_token ~ '[^[:space:]]'),
    status varchar(16) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD', 'CANCELLED')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at timestamp(6) with time zone,
    processing_started_at timestamp(6) with time zone,
    claim_token uuid,
    sent_at timestamp(6) with time zone,
    last_error varchar(3000),
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT fk_auth_email_deliveries_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE notifications (
    id uuid PRIMARY KEY,
    recipient_id uuid NOT NULL,
    title varchar(255) NOT NULL CHECK (title ~ '[^[:space:]]'),
    body varchar(3000) NOT NULL CHECK (body ~ '[^[:space:]]'),
    resource_type varchar(255) NOT NULL CHECK (resource_type IN ('EVENT', 'THREAD', 'FILE', 'CONVERSATION', 'USER')),
    resource_id uuid NOT NULL,
    parent_resource_type varchar(255) CHECK (parent_resource_type IN ('EVENT', 'THREAD', 'FILE', 'CONVERSATION', 'USER')),
    parent_resource_id uuid,
    created_at timestamp(6) with time zone NOT NULL,
    read_at timestamp(6) with time zone,
    CONSTRAINT chk_notifications_parent_reference CHECK (
        (parent_resource_type IS NULL AND parent_resource_id IS NULL)
        OR (parent_resource_type IS NOT NULL AND parent_resource_id IS NOT NULL)),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE notification_deliveries (
    id uuid PRIMARY KEY,
    notification_id uuid NOT NULL,
    channel varchar(255) NOT NULL CHECK (channel IN ('PUSH_MOBILE', 'PUSH_WEB', 'EMAIL')),
    target_key varchar(320) NOT NULL CHECK (target_key ~ '[^[:space:]]'),
    target_email varchar(320),
    target_device_id uuid,
    target_installation_id varchar(255),
    status varchar(255) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD', 'SKIPPED')),
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at timestamp(6) with time zone,
    processing_started_at timestamp(6) with time zone,
    claim_token uuid,
    sent_at timestamp(6) with time zone,
    provider_message_id varchar(255),
    last_error varchar(3000),
    created_at timestamp(6) with time zone NOT NULL,
    CONSTRAINT uq_notification_deliveries_target UNIQUE (notification_id, channel, target_key),
    CONSTRAINT fk_notification_deliveries_notification FOREIGN KEY (notification_id) REFERENCES notifications ON DELETE CASCADE
);

-- Delivery target IDs are snapshots, deliberately without device foreign keys.
CREATE TABLE notification_devices (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    platform varchar(255) NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    firebase_installation_id varchar(255) NOT NULL CHECK (firebase_installation_id ~ '[^[:space:]]'),
    created_at timestamp(6) with time zone NOT NULL,
    last_seen_at timestamp(6) with time zone,
    CONSTRAINT uq_notification_devices_firebase_installation_id UNIQUE (firebase_installation_id),
    CONSTRAINT fk_notification_devices_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE TABLE notification_preferences (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    resource_type varchar(255) NOT NULL CHECK (resource_type IN ('EVENT', 'THREAD', 'FILE', 'CONVERSATION', 'USER')),
    channel varchar(255) NOT NULL CHECK (channel IN ('PUSH_MOBILE', 'PUSH_WEB', 'EMAIL')),
    enabled boolean NOT NULL,
    CONSTRAINT uq_notification_preferences_user_resource_channel UNIQUE (user_id, resource_type, channel),
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users ON DELETE CASCADE
);

CREATE INDEX idx_events_owner_start_id ON events (user_id, event_start_date DESC, id DESC);
CREATE INDEX idx_events_city_start_id ON events (city_id, event_start_date DESC, id DESC);
CREATE INDEX idx_event_user_user_event ON event_user (user_id, event_id);
CREATE INDEX idx_event_tag_tag_event ON event_tag (tag_id, event_id);
CREATE INDEX idx_files_event_upload_id ON files (event_id, upload_date_time ASC, id ASC);
CREATE INDEX idx_threads_event_last_activity_id ON threads (event_id, last_activity DESC, id DESC);
CREATE INDEX idx_thread_replies_thread_date_id ON thread_replies (thread_id, reply_date ASC, id DESC);
CREATE INDEX idx_message_conversation_created ON messages (conversation_id, sent_date DESC, id DESC);
CREATE INDEX idx_conversation_participant_user ON conversation_participant (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_device_type ON refresh_tokens (user_id, device_type);
CREATE INDEX idx_users_lower_email ON users ((lower(email)));
CREATE INDEX idx_threads_user ON threads (user_id);
CREATE INDEX idx_thread_replies_user ON thread_replies (user_id);
CREATE INDEX idx_files_user ON files (user_id);
CREATE INDEX idx_messages_sender ON messages (sender_id);
CREATE INDEX idx_direct_conversation_pair_second_user ON direct_conversation_pair (second_user_id);
CREATE INDEX idx_auth_email_deliveries_processable
    ON auth_email_deliveries (status, next_attempt_at, processing_started_at, created_at)
    WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');
CREATE INDEX idx_auth_email_deliveries_user_type_created
    ON auth_email_deliveries (user_id, type, created_at DESC);
CREATE INDEX idx_notifications_recipient_created_at ON notifications (recipient_id, created_at DESC, id DESC);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id) WHERE read_at IS NULL;
CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notification_deliveries_processable
    ON notification_deliveries (status, next_attempt_at, processing_started_at, created_at)
    WHERE status IN ('PENDING', 'FAILED', 'PROCESSING');
CREATE INDEX idx_notification_deliveries_claim_token ON notification_deliveries (claim_token) WHERE claim_token IS NOT NULL;
CREATE INDEX idx_notification_deliveries_target_device ON notification_deliveries (target_device_id) WHERE target_device_id IS NOT NULL;
CREATE INDEX idx_notification_deliveries_dead_created_at ON notification_deliveries (created_at) WHERE status = 'DEAD';
CREATE INDEX idx_notification_devices_user_platform ON notification_devices (user_id, platform);
