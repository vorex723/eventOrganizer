package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.tag.Tag;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating Tag test objects with sensible defaults.
 */
public class TagTestBuilder {

    private UUID id = TagConstants.FIRST_TAG_ID;
    private String name = TagConstants.FIRST_TAG_NAME;

    public static TagTestBuilder firstTag() {
        return new TagTestBuilder()
                .id(TagConstants.FIRST_TAG_ID)
                .name(TagConstants.FIRST_TAG_NAME);
    }

    public static TagTestBuilder secondTag() {
        return new TagTestBuilder()
                .id(TagConstants.SECOND_TAG_ID)
                .name(TagConstants.SECOND_TAG_NAME);
    }

    public static TagTestBuilder thirdTag() {
        return new TagTestBuilder()
                .id(TagConstants.THIRD_TAG_ID)
                .name(TagConstants.THIRD_TAG_NAME);
    }

    public TagTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public TagTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public Tag build() {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}