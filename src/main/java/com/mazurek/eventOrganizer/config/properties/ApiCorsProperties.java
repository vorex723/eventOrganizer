package com.mazurek.eventOrganizer.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.api.cors")
public class ApiCorsProperties {

    /**
     * Explicit browser origins permitted to call the API. An empty list keeps
     * the API same-origin only.
     */
    private List<String> allowedOrigins = new ArrayList<>();
}
