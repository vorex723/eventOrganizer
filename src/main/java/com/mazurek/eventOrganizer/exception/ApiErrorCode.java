package com.mazurek.eventOrganizer.exception;

public final class ApiErrorCode {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    public static final String INVALID_PAGE_NUMBER = "INVALID_PAGE_NUMBER";
    public static final String REQUEST_IO_ERROR = "REQUEST_IO_ERROR";
    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    public static final String ACTIVATION_TOKEN_NOT_FOUND = "ACTIVATION_TOKEN_NOT_FOUND";
    public static final String PASSWORD_RESET_TOKEN_INVALID = "PASSWORD_RESET_TOKEN_INVALID";
    public static final String ACTIVATION_TOKEN_EXPIRED = "ACTIVATION_TOKEN_EXPIRED";
    public static final String ACCOUNT_ALREADY_ACTIVATED = "ACCOUNT_ALREADY_ACTIVATED";
    public static final String EMAIL_CHANGE_ADDRESS_UNAVAILABLE = "EMAIL_CHANGE_ADDRESS_UNAVAILABLE";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String REFRESH_TOKEN_NOT_FOUND = "REFRESH_TOKEN_NOT_FOUND";
    public static final String REFRESH_TOKEN_EXPIRED = "REFRESH_TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_REVOKED = "REFRESH_TOKEN_REVOKED";

    public static final String INVALID_USER = "INVALID_USER";
    public static final String INVALID_CURRENT_PASSWORD = "INVALID_CURRENT_PASSWORD";
    public static final String PASSWORD_CONFIRMATION_MISMATCH = "PASSWORD_CONFIRMATION_MISMATCH";
    public static final String EMAIL_CONFIRMATION_MISMATCH = "EMAIL_CONFIRMATION_MISMATCH";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String EMAIL_UNCHANGED = "EMAIL_UNCHANGED";
    public static final String INVALID_EMAIL = "INVALID_EMAIL";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String USER_BANNED = "USER_BANNED";
    public static final String ACCOUNT_NOT_ACTIVATED = "ACCOUNT_NOT_ACTIVATED";

    public static final String CITY_NOT_FOUND = "CITY_NOT_FOUND";
    public static final String TAG_NOT_FOUND = "TAG_NOT_FOUND";
    public static final String CONVERSATION_NOT_FOUND = "CONVERSATION_NOT_FOUND";
    public static final String CANNOT_MESSAGE_SELF = "CANNOT_MESSAGE_SELF";

    public static final String EVENT_NOT_FOUND = "EVENT_NOT_FOUND";
    public static final String INVALID_EVENT_START_DATE = "INVALID_EVENT_START_DATE";
    public static final String NOT_EVENT_OWNER = "NOT_EVENT_OWNER";
    public static final String EVENT_ALREADY_STARTED = "EVENT_ALREADY_STARTED";
    public static final String EVENT_OWNER_CANNOT_ATTEND = "EVENT_OWNER_CANNOT_ATTEND";
    public static final String ALREADY_ATTENDING_EVENT = "ALREADY_ATTENDING_EVENT";
    public static final String EVENT_FULL = "EVENT_FULL";
    public static final String EVENT_CAPACITY_BELOW_ATTENDEES = "EVENT_CAPACITY_BELOW_ATTENDEES";
    public static final String EVENT_OWNER_CANNOT_LEAVE = "EVENT_OWNER_CANNOT_LEAVE";
    public static final String NOT_EVENT_ATTENDEE = "NOT_EVENT_ATTENDEE";

    public static final String EMPTY_UPLOADED_FILE = "EMPTY_UPLOADED_FILE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String FILE_TYPE_NOT_ALLOWED = "FILE_TYPE_NOT_ALLOWED";
    public static final String EVENT_FILE_QUOTA_EXCEEDED = "EVENT_FILE_QUOTA_EXCEEDED";
    public static final String FILE_NOT_FOUND = "FILE_NOT_FOUND";
    public static final String FILE_NOT_FOUND_IN_EVENT = "FILE_NOT_FOUND_IN_EVENT";

    public static final String THREAD_NOT_FOUND_IN_EVENT = "THREAD_NOT_FOUND_IN_EVENT";
    public static final String REPLY_NOT_FOUND_IN_THREAD = "REPLY_NOT_FOUND_IN_THREAD";
    public static final String THREAD_NOT_FOUND = "THREAD_NOT_FOUND";
    public static final String THREAD_REPLY_NOT_FOUND = "THREAD_REPLY_NOT_FOUND";
    public static final String NOT_THREAD_OWNER = "NOT_THREAD_OWNER";
    public static final String THREAD_DOES_NOT_BELONG_TO_EVENT = "THREAD_DOES_NOT_BELONG_TO_EVENT";
    public static final String NOT_THREAD_REPLY_OWNER = "NOT_THREAD_REPLY_OWNER";

    public static final String NOTIFICATION_NOT_FOUND = "NOTIFICATION_NOT_FOUND";
    public static final String INVALID_NOTIFICATION_PREFERENCES = "INVALID_NOTIFICATION_PREFERENCES";
    public static final String STALE_NOTIFICATION_PREFERENCES = "STALE_NOTIFICATION_PREFERENCES";

    private ApiErrorCode() {
    }

}
