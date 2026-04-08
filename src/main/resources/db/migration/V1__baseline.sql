create table activation_tokens (
    expiration_date timestamp(6) with time zone not null,
    id bigserial not null,
    token uuid not null unique,
    user_id uuid not null unique,
    primary key (id)
);

create table cities (
    id uuid not null,
    name varchar(255),
    primary key (id),
    constraint UKl61tawv0e2a93es77jkyvi7qa unique (name)
);

create table conversations (
    id uuid not null,
    primary key (id)
);

create table event_tag (
    event_id uuid not null,
    tag_id uuid not null,
    primary key (event_id, tag_id)
);

create table event_user (
    event_id uuid not null,
    user_id uuid not null,
    primary key (event_id, user_id)
);

create table events (
    create_date timestamp(6) with time zone,
    event_start_date timestamp(6) with time zone,
    last_update timestamp(6) with time zone,
    city_id uuid,
    id uuid not null,
    user_id uuid,
    exact_address varchar(255),
    long_description text,
    name varchar(255),
    short_description varchar(255),
    time_zone_id varchar(255),
    primary key (id)
);

create table files (
    upload_date_time timestamp(6) with time zone,
    event_id uuid,
    id uuid not null,
    user_id uuid,
    content_type varchar(255),
    original_file_name varchar(255),
    owner_name_at_creation varchar(255),
    user_file_name varchar(255),
    content bytea,
    primary key (id)
);

create table messages (
    sent_date timestamp(6),
    conversation_id uuid,
    id uuid not null,
    sender_id uuid,
    message varchar(255),
    primary key (id)
);

create table notifications (
    opened boolean,
    create_date timestamp(6),
    id uuid not null,
    resource_id uuid,
    user_id uuid,
    body varchar(255),
    title varchar(255),
    type varchar(255) check (type in ('EVENT_UPDATE','EVENT_NEW_FILE','EVENT_NEW_THREAD','THREAD_REPLY','PRIVATE_MESSAGE')),
    primary key (id)
);

create table refresh_tokens (
    revoked boolean not null,
    created_at timestamp(6) with time zone not null,
    expiry_date timestamp(6) with time zone not null,
    id bigserial not null,
    last_used_at timestamp(6) with time zone not null,
    user_id uuid not null,
    device_info varchar(255),
    device_type varchar(255) not null check (device_type in ('MOBILE_ANDROID','MOBILE_IOS','MOBILE_OTHER','TABLET_ANDROID','TABLET_IOS','TABLET_OTHER','DESKTOP','WEB','UNKNOWN')),
    token varchar(255) not null unique,
    primary key (id)
);

create table roles (
    id bigserial not null,
    name varchar(255) not null unique,
    primary key (id)
);

create table tags (
    id uuid not null,
    name varchar(255),
    primary key (id)
);

create table thread_replies (
    edit_counter integer,
    last_update timestamp(6) with time zone,
    reply_date timestamp(6) with time zone,
    id uuid not null,
    thread_id uuid,
    user_id uuid,
    content varchar(255),
    replier_name_at_creation varchar(255),
    primary key (id)
);

create table threads (
    edit_counter integer,
    create_date timestamp(6) with time zone,
    last_update timestamp(6) with time zone,
    event_id uuid,
    id uuid not null,
    user_id uuid,
    content varchar(255),
    name varchar(255),
    owner_name_at_creation varchar(255),
    primary key (id)
);

create table user_conversation (
    conversation_id uuid not null,
    user_id uuid not null,
    primary key (conversation_id, user_id)
);

create table user_roles (
    role_id bigint not null,
    user_id uuid not null,
    primary key (role_id, user_id)
);

create table users (
    activated boolean not null,
    banned boolean not null,
    created_at timestamp(6) with time zone not null,
    last_credentials_change_time timestamp(6) with time zone not null,
    city_id uuid not null,
    id uuid not null,
    email varchar(255) not null unique,
    fcm_android_token varchar(255),
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    password varchar(255) not null,
    time_zone varchar(255) not null,
    primary key (id)
);

alter table if exists activation_tokens
    add constraint FKgtry5rof27b5hdur1a11y0gsn foreign key (user_id) references users;

alter table if exists event_tag
    add constraint FK21sr8312wkacwoig6477juer3 foreign key (tag_id) references tags;

alter table if exists event_tag
    add constraint FK90aj6q3nxe0mqa2sdh35its85 foreign key (event_id) references events;

alter table if exists event_user
    add constraint FK45uvxaov8fbham8l63a7jkbyr foreign key (user_id) references users;

alter table if exists event_user
    add constraint FKdip9gnrj5k5vd2jlyjmxrnlch foreign key (event_id) references events;

alter table if exists events
    add constraint FKoqicyi54u66pj29af47da15xk foreign key (city_id) references cities;

alter table if exists events
    add constraint FKat8p3s7yjcp57lny4udqvqncq foreign key (user_id) references users;

alter table if exists files
    add constraint FKeitemc76qh2qouafitttwdaan foreign key (event_id) references events;

alter table if exists files
    add constraint FKdgr5hx49828s5vhjo1s8q3wdp foreign key (user_id) references users;

alter table if exists messages
    add constraint FKt492th6wsovh1nush5yl5jj8e foreign key (conversation_id) references conversations;

alter table if exists messages
    add constraint FK4ui4nnwntodh6wjvck53dbk9m foreign key (sender_id) references users;

alter table if exists notifications
    add constraint FK9y21adhxn0ayjhfocscqox7bh foreign key (user_id) references users;

alter table if exists refresh_tokens
    add constraint FK1lih5y2npsf8u5o3vhdb9y0os foreign key (user_id) references users;

alter table if exists thread_replies
    add constraint FKepxueiufvmrw5sos0p78hxjhs foreign key (user_id) references users;

alter table if exists thread_replies
    add constraint FK8tbwkf6bhbk5eyqcef2kni1w2 foreign key (thread_id) references threads;

alter table if exists threads
    add constraint FKmvohss0haq04hf49888xhayif foreign key (event_id) references events;

alter table if exists threads
    add constraint FKovbl0s79udv66qtffqr234iup foreign key (user_id) references users;

alter table if exists user_conversation
    add constraint FK3bbvp9frombpaamxt90429k1v foreign key (conversation_id) references conversations;

alter table if exists user_conversation
    add constraint FK1ku1u4d2bmps059vypee7c702 foreign key (user_id) references users;

alter table if exists user_roles
    add constraint FKh8ciramu9cc9q3qcqiv4ue8a6 foreign key (role_id) references roles;

alter table if exists user_roles
    add constraint FKhfh9dx7w3ubf1co1vdev94g3f foreign key (user_id) references users;

alter table if exists users
    add constraint FKn36jwt4acj3il2ixvv2c0ncco foreign key (city_id) references cities;
