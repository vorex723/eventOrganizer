package com.mazurek.eventOrganizer.validators;

import java.util.regex.Pattern;

public final class LookupNameValidator {

    private static final Pattern LOOKUP_NAME = Pattern.compile(ValidationConstraints.LOOKUP_NAME_PATTERN);

    private LookupNameValidator() {
    }

    public static void requireValid(String value, int minLength, int maxLength, String name) {
        if (value == null || value.length() < minLength || value.length() > maxLength
                || !LOOKUP_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    name + " must contain only letters, numbers, spaces, hyphens, or apostrophes."
            );
        }
    }
}
