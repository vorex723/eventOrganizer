package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.utils.EncryptionUtils.EncryptedConversationContent;

import static com.mazurek.eventOrganizer.testData.TestConstants.EncryptionFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class EncryptedConversationContentTestBuilder {
    private String keyId = EncryptionFixtureConstants.KEY_ID;
    private String ciphertext = MessageConstants.ENCRYPTED_FIRST_MESSAGE_CONTENT;

    public EncryptedConversationContentTestBuilder keyId(String keyId) {
        this.keyId = keyId;
        return this;
    }

    public EncryptedConversationContentTestBuilder ciphertext(String ciphertext) {
        this.ciphertext = ciphertext;
        return this;
    }


    public EncryptedConversationContent build() {
        return new EncryptedConversationContent(
                keyId,
                ciphertext
        );
    }
}
