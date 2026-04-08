package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    @NotBlank
    private String activationBaseUrl;

    @NotBlank
    private String fromAddress;
}
