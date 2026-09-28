alter table activation_tokens
    drop constraint if exists fkgtry5rof27b5hdur1a11y0gsn,
    add constraint fk_activation_tokens_user
        foreign key (user_id) references users on delete cascade;

alter table password_reset_tokens
    drop constraint if exists fk_password_reset_tokens_user,
    add constraint fk_password_reset_tokens_user
        foreign key (user_id) references users on delete cascade;

alter table email_change_tokens
    drop constraint if exists fk_email_change_tokens_user,
    add constraint fk_email_change_tokens_user
        foreign key (user_id) references users on delete cascade;

alter table auth_email_deliveries
    drop constraint if exists fk_auth_email_deliveries_user,
    add constraint fk_auth_email_deliveries_user
        foreign key (user_id) references users on delete cascade;

alter table refresh_tokens
    drop constraint if exists fk1lih5y2npsf8u5o3vhdb9y0os,
    add constraint fk_refresh_tokens_user
        foreign key (user_id) references users on delete cascade;

alter table notifications
    drop constraint if exists fk_notifications_recipient,
    add constraint fk_notifications_recipient
        foreign key (recipient_id) references users on delete cascade;

alter table event_user
    drop constraint if exists fk45uvxaov8fbham8l63a7jkbyr,
    add constraint fk_event_user_user
        foreign key (user_id) references users on delete cascade;

alter table user_roles
    drop constraint if exists fkhfh9dx7w3ubf1co1vdev94g3f,
    add constraint fk_user_roles_user
        foreign key (user_id) references users on delete cascade;

alter table direct_conversation_pair
    drop constraint if exists fk_direct_conversation_pair_first_user,
    add constraint fk_direct_conversation_pair_first_user
        foreign key (first_user_id) references users on delete cascade,
    drop constraint if exists fk_direct_conversation_pair_second_user,
    add constraint fk_direct_conversation_pair_second_user
        foreign key (second_user_id) references users on delete cascade;

alter table events
    drop constraint if exists fkat8p3s7yjcp57lny4udqvqncq,
    add constraint fk_events_owner
        foreign key (user_id) references users on delete set null;

alter table files
    drop constraint if exists fkdgr5hx49828s5vhjo1s8q3wdp,
    add constraint fk_files_owner
        foreign key (user_id) references users on delete set null;

alter table threads
    drop constraint if exists fkovbl0s79udv66qtffqr234iup,
    add constraint fk_threads_owner
        foreign key (user_id) references users on delete set null;

alter table thread_replies
    drop constraint if exists fkepxueiufvmrw5sos0p78hxjhs,
    add constraint fk_thread_replies_replier
        foreign key (user_id) references users on delete set null;
