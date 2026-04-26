package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    @Positive
    private long accessExpiration;

    @Positive
    private long refreshShortExpiration;

    @Positive
    private long refreshLongExpiration;

    @NotBlank
    private String secret;
}
