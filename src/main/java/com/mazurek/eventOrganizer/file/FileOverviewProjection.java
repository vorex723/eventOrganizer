package com.mazurek.eventOrganizer.file;

import java.time.Instant;
import java.util.UUID;

public interface FileOverviewProjection {

    UUID getId();
    String getUserFileName();
    String getOriginalFileName();
    String getContentType();
    Instant getUploadDateTime();
    UUID getOwnerId();
    String getOwnerFirstName();
    String getOwnerLastName();
    String getOwnerHomeCity();
}
