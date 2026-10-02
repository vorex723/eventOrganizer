package com.mazurek.eventOrganizer.testData;

import java.util.Locale;

/** Synthetic provider identifiers for offline tests only; never used as persisted production defaults. */
public final class TestCityData {
    private TestCityData() {}

    public static String externalId(String nameOrId) {
        if (nameOrId == null || nameOrId.isBlank() || nameOrId.startsWith("test:")) return nameOrId;
        return "test:" + nameOrId.strip().toLowerCase(Locale.ROOT);
    }

    public static String name(String externalId) {
        return externalId.startsWith("test:") ? externalId.substring(5) : externalId;
    }
}
