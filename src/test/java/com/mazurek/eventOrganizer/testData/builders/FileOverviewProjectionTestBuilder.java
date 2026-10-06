package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.file.FileOverviewProjection;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/** A detached snapshot, not a mutable view of an incidental File entity. */
public class FileOverviewProjectionTestBuilder {
    private UUID id = FileConstants.JPG_FILE_ID;
    private String userFileName = FileConstants.FIRST_FILE_USER_NAME;
    private String originalFileName = FileConstants.JPG_FILE_ORIGINAL_NAME;
    private String contentType = FileConstants.JPG_FILE_CONTENT_TYPE;
    private Instant uploadDateTime = TimeConstants.NOW;
    private UUID ownerId = UserConstants.FIRST_USER_ID;
    private String ownerFirstName = UserConstants.FIRST_USER_FIRST_NAME;
    private String ownerLastName = UserConstants.FIRST_USER_LAST_NAME;

    public FileOverviewProjectionTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public FileOverviewProjectionTestBuilder userFileName(String userFileName) {
        this.userFileName = userFileName;
        return this;
    }

    public FileOverviewProjectionTestBuilder originalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
        return this;
    }

    public FileOverviewProjectionTestBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public FileOverviewProjectionTestBuilder uploadDateTime(Instant uploadDateTime) {
        this.uploadDateTime = uploadDateTime;
        return this;
    }

    public FileOverviewProjectionTestBuilder ownerId(UUID ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public FileOverviewProjectionTestBuilder ownerFirstName(String ownerFirstName) {
        this.ownerFirstName = ownerFirstName;
        return this;
    }

    public FileOverviewProjectionTestBuilder ownerLastName(String ownerLastName) {
        this.ownerLastName = ownerLastName;
        return this;
    }

    public FileOverviewProjection build() {
        return new Snapshot(id, userFileName, originalFileName, contentType, uploadDateTime, ownerId, ownerFirstName, ownerLastName);
    }

    private record Snapshot(UUID id, String userFileName, String originalFileName, String contentType, Instant uploadDateTime, UUID ownerId, String ownerFirstName, String ownerLastName) implements FileOverviewProjection {
        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public String getUserFileName() {
            return userFileName;
        }

        @Override
        public String getOriginalFileName() {
            return originalFileName;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public Instant getUploadDateTime() {
            return uploadDateTime;
        }

        @Override
        public UUID getOwnerId() {
            return ownerId;
        }

        @Override
        public String getOwnerFirstName() {
            return ownerFirstName;
        }

        @Override
        public String getOwnerLastName() {
            return ownerLastName;
        }

    }
}
