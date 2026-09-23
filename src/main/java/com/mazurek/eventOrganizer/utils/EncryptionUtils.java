package com.mazurek.eventOrganizer.utils;


import lombok.RequiredArgsConstructor;
import com.mazurek.eventOrganizer.config.properties.EncryptionProperties;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

import java.util.Optional;


@RequiredArgsConstructor
@Component
public class EncryptionUtils {

    private final TextEncryptor textEncryptor;
    private final EncryptionProperties encryptionProperties;

    public String encryptMessage(String originalMessage){
        return textEncryptor.encrypt(originalMessage);
    }
    public String decryptMessage(String encryptedMessage){
        return textEncryptor.decrypt(encryptedMessage);
    }

    public EncryptedConversationContent encryptConversationMessage(String originalMessage) {
        String keyId = encryptionProperties.getMessageActiveKeyId();
        return new EncryptedConversationContent(keyId, encryptWithMessageKey(originalMessage, keyId));
    }

    public Optional<String> decryptConversationMessage(String encryptedMessage, String keyId) {
        try {
            return Optional.of(decryptWithMessageKey(encryptedMessage, keyId));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String encryptWithMessageKey(String message, String keyId) {
        return messageEncryptor(keyId).encrypt(message);
    }

    private String decryptWithMessageKey(String message, String keyId) {
        return messageEncryptor(keyId).decrypt(message);
    }

    private TextEncryptor messageEncryptor(String keyId) {
        EncryptionProperties.EncryptionKey key = encryptionProperties.resolveMessageKey(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Message encryption key is not configured: " + keyId);
        }
        return Encryptors.delux(key.getPassword(), key.getSalt());
    }

    public record EncryptedConversationContent(String keyId, String ciphertext) {
    }

}
