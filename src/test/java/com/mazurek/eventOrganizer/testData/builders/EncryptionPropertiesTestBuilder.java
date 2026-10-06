package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.EncryptionProperties;
import com.mazurek.eventOrganizer.config.properties.EncryptionProperties.EncryptionKey;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.mazurek.eventOrganizer.testData.TestConstants.EncryptionFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class EncryptionPropertiesTestBuilder {
    private String password = EncryptionFixtureConstants.PASSWORD;
    private String salt = EncryptionFixtureConstants.SALT;
    private String messageActiveKeyId = EncryptionFixtureConstants.KEY_ID;
    private Map<String, EncryptionKey> messageKeys = Map.of();

    public EncryptionPropertiesTestBuilder password(String password) {
        this.password = password;
        return this;
    }

    public EncryptionPropertiesTestBuilder salt(String salt) {
        this.salt = salt;
        return this;
    }

    public EncryptionPropertiesTestBuilder messageActiveKeyId(String messageActiveKeyId) {
        this.messageActiveKeyId = messageActiveKeyId;
        return this;
    }

    public EncryptionPropertiesTestBuilder messageKeys(Map<String, EncryptionKey> messageKeys) {
        this.messageKeys = copyMessageKeys(messageKeys);
        return this;
    }

    public static EncryptionProperties copyOf(EncryptionProperties source) {
        if (source == null) return null;
        return new EncryptionPropertiesTestBuilder()
                .password(source.getPassword())
                .salt(source.getSalt())
                .messageActiveKeyId(source.getMessageActiveKeyId())
                .messageKeys(source.getMessageKeys())
                .build();
    }

    private static Map<String, EncryptionKey> copyMessageKeys(
            Map<String, EncryptionKey> source) {
        if (source == null) return null;
        Map<String, EncryptionKey> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> copy.put(key, EncryptionKeyTestBuilder.copyOf(value)));
        return copy;
    }


    public EncryptionProperties build() {
        EncryptionProperties value = new EncryptionProperties();
        value.setPassword(password);
        value.setSalt(salt);
        value.setMessageActiveKeyId(messageActiveKeyId);
        value.setMessageKeys(copyMessageKeys(messageKeys));
        return value;
    }
}
