package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.EncryptionProperties.EncryptionKey;

import static com.mazurek.eventOrganizer.testData.TestConstants.EncryptionFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class EncryptionKeyTestBuilder {
    private String password = EncryptionFixtureConstants.PASSWORD;
    private String salt = EncryptionFixtureConstants.SALT;

    public EncryptionKeyTestBuilder password(String password) {
        this.password = password;
        return this;
    }

    public EncryptionKeyTestBuilder salt(String salt) {
        this.salt = salt;
        return this;
    }

    public static EncryptionKey copyOf(EncryptionKey source) {
        if (source == null) return null;
        return new EncryptionKeyTestBuilder()
                .password(source.getPassword())
                .salt(source.getSalt())
                .build();
    }


    public EncryptionKey build() {
        EncryptionKey value = new EncryptionKey();
        value.setPassword(password);
        value.setSalt(salt);
        return value;
    }
}
