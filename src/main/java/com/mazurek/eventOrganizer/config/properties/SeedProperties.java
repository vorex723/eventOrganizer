package com.mazurek.eventOrganizer.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    private boolean localDataEnabled;
}
