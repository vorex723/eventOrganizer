package com.mazurek.eventOrganizer.utils;


import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
public class EncryptionUtils {

    private final TextEncryptor textEncryptor;

    public String encryptMessage(String originalMessage){
        return textEncryptor.encrypt(originalMessage);
    }
    public String decryptMessage(String encryptedMessage){
        return textEncryptor.decrypt(encryptedMessage);
    }

}
