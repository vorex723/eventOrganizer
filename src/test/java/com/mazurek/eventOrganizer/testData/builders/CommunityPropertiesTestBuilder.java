package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.CommunityProperties;
import org.springframework.util.unit.DataSize;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class CommunityPropertiesTestBuilder {
    private int maxTagsPerEvent = PropertyFixtureConstants.MAX_TAGS_PER_EVENT;
    private int maxFilesPerEvent = PropertyFixtureConstants.MAX_FILES_PER_EVENT;
    private DataSize maxFileSize = PropertyFixtureConstants.MAX_FILE_SIZE;
    private DataSize maxEventFileStorage = PropertyFixtureConstants.MAX_EVENT_FILE_STORAGE;
    private int maxDisplayFilenameLength = PropertyFixtureConstants.MAX_DISPLAY_FILENAME_LENGTH;
    private int maxAttendees = PropertyFixtureConstants.MAX_ATTENDEES;
    private int maxArchiveEntries = PropertyFixtureConstants.MAX_ARCHIVE_ENTRIES;
    private DataSize maxArchiveUncompressedSize = PropertyFixtureConstants.MAX_ARCHIVE_UNCOMPRESSED_SIZE;
    private int maxArchiveCompressionRatio = PropertyFixtureConstants.MAX_ARCHIVE_COMPRESSION_RATIO;

    public CommunityPropertiesTestBuilder maxTagsPerEvent(int maxTagsPerEvent) {
        this.maxTagsPerEvent = maxTagsPerEvent;
        return this;
    }

    public CommunityPropertiesTestBuilder maxFilesPerEvent(int maxFilesPerEvent) {
        this.maxFilesPerEvent = maxFilesPerEvent;
        return this;
    }

    public CommunityPropertiesTestBuilder maxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
        return this;
    }

    public CommunityPropertiesTestBuilder maxEventFileStorage(DataSize maxEventFileStorage) {
        this.maxEventFileStorage = maxEventFileStorage;
        return this;
    }

    public CommunityPropertiesTestBuilder maxDisplayFilenameLength(int maxDisplayFilenameLength) {
        this.maxDisplayFilenameLength = maxDisplayFilenameLength;
        return this;
    }

    public CommunityPropertiesTestBuilder maxAttendees(int maxAttendees) {
        this.maxAttendees = maxAttendees;
        return this;
    }

    public CommunityPropertiesTestBuilder maxArchiveEntries(int maxArchiveEntries) {
        this.maxArchiveEntries = maxArchiveEntries;
        return this;
    }

    public CommunityPropertiesTestBuilder maxArchiveUncompressedSize(DataSize maxArchiveUncompressedSize) {
        this.maxArchiveUncompressedSize = maxArchiveUncompressedSize;
        return this;
    }

    public CommunityPropertiesTestBuilder maxArchiveCompressionRatio(int maxArchiveCompressionRatio) {
        this.maxArchiveCompressionRatio = maxArchiveCompressionRatio;
        return this;
    }

    public static CommunityProperties copyOf(CommunityProperties source) {
        if (source == null) return null;
        return new CommunityPropertiesTestBuilder()
                .maxTagsPerEvent(source.getMaxTagsPerEvent())
                .maxFilesPerEvent(source.getMaxFilesPerEvent())
                .maxFileSize(source.getMaxFileSize())
                .maxEventFileStorage(source.getMaxEventFileStorage())
                .maxDisplayFilenameLength(source.getMaxDisplayFilenameLength())
                .maxAttendees(source.getMaxAttendees())
                .maxArchiveEntries(source.getMaxArchiveEntries())
                .maxArchiveUncompressedSize(source.getMaxArchiveUncompressedSize())
                .maxArchiveCompressionRatio(source.getMaxArchiveCompressionRatio())
                .build();
    }


    public CommunityProperties build() {
        CommunityProperties value = new CommunityProperties();
        value.setMaxTagsPerEvent(maxTagsPerEvent);
        value.setMaxFilesPerEvent(maxFilesPerEvent);
        value.setMaxFileSize(maxFileSize);
        value.setMaxEventFileStorage(maxEventFileStorage);
        value.setMaxDisplayFilenameLength(maxDisplayFilenameLength);
        value.setMaxAttendees(maxAttendees);
        value.setMaxArchiveEntries(maxArchiveEntries);
        value.setMaxArchiveUncompressedSize(maxArchiveUncompressedSize);
        value.setMaxArchiveCompressionRatio(maxArchiveCompressionRatio);
        return value;
    }
}
