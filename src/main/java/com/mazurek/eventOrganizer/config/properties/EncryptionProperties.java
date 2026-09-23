package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.encryption")
public class EncryptionProperties {

    @NotBlank
    private String password;

    @NotBlank
    @Pattern(
            regexp = "^([0-9a-fA-F]{2}){8,}$",
            message = "must be a hex string with an even number of characters, for example output from `openssl rand -hex 32`")
    private String salt;

    /**
     * The key used for newly persisted conversation messages. Existing rows without
     * a key id are treated as having been encrypted with the default key.
     */
    @NotBlank
    private String messageActiveKeyId = "default";

    @Valid
    private Map<String, EncryptionKey> messageKeys = new LinkedHashMap<>();

    public EncryptionKey resolveMessageKey(String keyId) {
        if ("default".equals(keyId)) {
            return new EncryptionKey(password, salt);
        }
        return messageKeys.get(keyId);
    }

    @AssertTrue(message = "app.encryption.message-active-key-id must identify a configured message key")
    public boolean isMessageActiveKeyConfigured() {
        EncryptionKey activeKey = resolveMessageKey(messageActiveKeyId);
        return activeKey != null
                && activeKey.getPassword() != null && !activeKey.getPassword().isBlank()
                && activeKey.getSalt() != null && !activeKey.getSalt().isBlank();
    }

    @Getter
    @Setter
    public static class EncryptionKey {

        @NotBlank
        private String password;

        @NotBlank
        @Pattern(
                regexp = "^([0-9a-fA-F]{2}){8,}$",
                message = "must be a hex string with an even number of characters, for example output from `openssl rand -hex 32`"
        )
        private String salt;

        public EncryptionKey() {
        }

        public EncryptionKey(String password, String salt) {
            this.password = password;
            this.salt = salt;
        }
    }
}
