package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.config.properties.EncryptionProperties;
import com.mazurek.eventOrganizer.testData.builders.EncryptionKeyTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.EncryptionPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EncryptionUtilsUnitTest contracts:")
class EncryptionUtilsUnitTest {

    private static final String DEFAULT_PASSWORD = com.mazurek.eventOrganizer.testData.TestConstants.EncryptionFixtureConstants.PASSWORD;
    private static final String DEFAULT_SALT = com.mazurek.eventOrganizer.testData.TestConstants.EncryptionFixtureConstants.SALT;
    private static final String ROTATED_PASSWORD = "9c303f800bcd32c6caa529391b6852e5bbf1e56753e72571db036fd3cd75ded1";
    private static final String ROTATED_SALT = "a07d0505ae201a45217968a3a10bdbd0a2e98a9a9bfa3011cfd17e6c7a120b85";

    @Test
    void whenMessageKeyIsRotatedShouldEncryptWithActiveKeyAndDecryptOlderKeys() {
        EncryptionProperties properties = properties();
        properties.setMessageActiveKeyId("rotated");
        properties.setMessageKeys(Map.of("rotated", new EncryptionKeyTestBuilder()
                .password(ROTATED_PASSWORD)
                .salt(ROTATED_SALT)
                .build()));
        EncryptionUtils encryptionUtils = new EncryptionUtils(
                Encryptors.delux(DEFAULT_PASSWORD, DEFAULT_SALT),
                properties
        );

        EncryptionUtils.EncryptedConversationContent encrypted = encryptionUtils.encryptConversationMessage("new message");
        String previousKeyCiphertext = Encryptors.delux(DEFAULT_PASSWORD, DEFAULT_SALT).encrypt("message encrypted with previous key");

        assertThat(encrypted).isNotNull();
        assertThat(encrypted.ciphertext()).isNotBlank().isNotEqualTo("new message");
        assertThat(encrypted.keyId()).isEqualTo("rotated");
        assertThat(encryptionUtils.decryptConversationMessage(encrypted.ciphertext(), encrypted.keyId()))
                .contains("new message");
        assertThat(encryptionUtils.decryptConversationMessage(previousKeyCiphertext, "default"))
                .contains("message encrypted with previous key");
    }

    @Test
    void whenKeyIsMissingOrCiphertextIsMalformedShouldReturnEmpty() {
        EncryptionUtils encryptionUtils = new EncryptionUtils(
                Encryptors.delux(DEFAULT_PASSWORD, DEFAULT_SALT),
                properties()
        );

        assertThat(encryptionUtils.decryptConversationMessage("not-ciphertext", "default")).isEmpty();
        assertThat(encryptionUtils.decryptConversationMessage("ciphertext", "retired")).isEmpty();
    }

    private EncryptionProperties properties() {
        return new EncryptionPropertiesTestBuilder()
                .password(DEFAULT_PASSWORD)
                .salt(DEFAULT_SALT)
                .messageActiveKeyId("default")
                .messageKeys(Map.of())
                .build();
    }
}
