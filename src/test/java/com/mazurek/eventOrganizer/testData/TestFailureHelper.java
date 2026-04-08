package com.mazurek.eventOrganizer.testData;

import java.util.Optional;

public final class TestFailureHelper {

    private TestFailureHelper() {
    }

    public static <T> T requirePresent(Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new AssertionError(message));
    }
}
