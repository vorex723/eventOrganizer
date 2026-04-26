package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
}
