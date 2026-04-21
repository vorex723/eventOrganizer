package com.mazurek.eventOrganizer.thread;

public enum ThreadSortField {
    CREATE_DATE("createDate"),
    LAST_ACTIVITY("lastActivity"),
    REPLY_COUNT("replyCount")
    ;

    private final String sortField;

    ThreadSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortField() {
        return sortField;
    }
}
