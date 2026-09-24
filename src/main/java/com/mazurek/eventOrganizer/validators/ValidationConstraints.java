package com.mazurek.eventOrganizer.validators;

public final class ValidationConstraints {

    public static final int EMAIL_MAX_LENGTH = 255;
    public static final int PASSWORD_MIN_LENGTH = 8;
    public static final int PASSWORD_MAX_LENGTH = 32;
    public static final int CITY_MIN_LENGTH = 3;
    public static final int CITY_MAX_LENGTH = 30;
    public static final int TAG_MIN_LENGTH = 2;
    public static final int TAG_MAX_LENGTH = 30;

    /**
     * Deliberately permits TLDs of any length while retaining the application's
     * existing ASCII local-part and dot-separated domain policy.
     */
    public static final String EMAIL_PATTERN =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$";

    public static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+!=])(?=\\S+$).{8,32}$";

    /**
     * Human-readable names that can safely be URL-encoded as a single path segment.
     */
    public static final String LOOKUP_NAME_PATTERN =
            "^[\\p{L}\\p{N}]+(?:[ '\\-][\\p{L}\\p{N}]+)*$";

    private ValidationConstraints() {
    }
}
