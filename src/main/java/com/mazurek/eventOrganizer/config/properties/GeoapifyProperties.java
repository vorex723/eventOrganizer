package com.mazurek.eventOrganizer.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.geoapify")
public record GeoapifyProperties(
        String apiKey,
        String baseUrl,
        int connectTimeout,
        int readTimeout
) {
}
