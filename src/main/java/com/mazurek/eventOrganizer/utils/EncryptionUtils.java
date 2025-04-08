package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.conversation.Conversation;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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

    public void decryptMessagesInConversation(Conversation conversation){
        conversation.getMessages().forEach(message -> message.setMessage(textEncryptor.decrypt(message.getMessage())));
    }
}
