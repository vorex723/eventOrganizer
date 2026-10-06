package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.file.FileContentProjection;
import com.mazurek.eventOrganizer.testData.TestFileContentFactory;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/** A detached snapshot, not a mutable view of an incidental File entity. */
public class FileContentProjectionTestBuilder {
    private String contentType = FileConstants.JPG_FILE_CONTENT_TYPE;
    private byte[] content = TestFileContentFactory.jpg();

    public FileContentProjectionTestBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public FileContentProjectionTestBuilder content(byte[] content) {
        this.content = content == null ? null : content.clone();
        return this;
    }

    public FileContentProjection build() {
        return new Snapshot(contentType, content == null ? null : content.clone());
    }

    private record Snapshot(String contentType, byte[] content) implements FileContentProjection {
        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public byte[] getContent() {
            return content == null ? null : content.clone();
        }

    }
}
