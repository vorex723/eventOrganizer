package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.config.properties.EncryptionProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.encrypt.Encryptors;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptionUtilsUnitTest {

    private static final String DEFAULT_PASSWORD = "63103f800bcd32c6caa529391b6852e5bbf1e56753e72571db036fd3cd75ded1";
    private static final String DEFAULT_SALT = "f07d0505ae201a45217968a3a10bdbd0a2e98a9a9bfa3011cfd17e6c7a120b85";
    private static final String ROTATED_PASSWORD = "9c303f800bcd32c6caa529391b6852e5bbf1e56753e72571db036fd3cd75ded1";
    private static final String ROTATED_SALT = "a07d0505ae201a45217968a3a10bdbd0a2e98a9a9bfa3011cfd17e6c7a120b85";

    @Test
    void encryptsWithTheConfiguredActiveConversationKeyAndDecryptsOlderKeys() {
        EncryptionProperties properties = properties();
        properties.setMessageActiveKeyId("rotated");
        properties.setMessageKeys(Map.of("rotated", new EncryptionProperties.EncryptionKey(ROTATED_PASSWORD, ROTATED_SALT)));
        EncryptionUtils encryptionUtils = new EncryptionUtils(
                Encryptors.delux(DEFAULT_PASSWORD, DEFAULT_SALT),
                properties
        );

        EncryptionUtils.EncryptedConversationContent encrypted = encryptionUtils.encryptConversationMessage("new message");
        String legacyCiphertext = Encryptors.delux(DEFAULT_PASSWORD, DEFAULT_SALT).encrypt("legacy message");

        assertThat(encrypted.keyId()).isEqualTo("rotated");
        assertThat(encryptionUtils.decryptConversationMessage(encrypted.ciphertext(), encrypted.keyId()))
                .contains("new message");
        assertThat(encryptionUtils.decryptConversationMessage(legacyCiphertext, "default"))
                .contains("legacy message");
    }

    @Test
    void returnsEmptyForMissingKeysOrMalformedConversationCiphertext() {
        EncryptionUtils encryptionUtils = new EncryptionUtils(
                Encryptors.delux(DEFAULT_PASSWORD, DEFAULT_SALT),
                properties()
        );

        assertThat(encryptionUtils.decryptConversationMessage("not-ciphertext", "default")).isEmpty();
        assertThat(encryptionUtils.decryptConversationMessage("ciphertext", "retired")).isEmpty();
    }

    private EncryptionProperties properties() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setPassword(DEFAULT_PASSWORD);
        properties.setSalt(DEFAULT_SALT);
        return properties;
    }
}
