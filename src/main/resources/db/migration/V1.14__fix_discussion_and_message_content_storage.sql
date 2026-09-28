alter table threads
    alter column content type varchar(1000);

alter table thread_replies
    alter column content type varchar(1000);

alter table messages
    alter column content type text;
