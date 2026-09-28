package com.mazurek.eventOrganizer.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendProperties {

    private URI url;

    public boolean isValidUrl() {
        return url != null
                && url.isAbsolute()
                && "https".equalsIgnoreCase(url.getScheme())
                && url.getHost() != null
                && url.getQuery() == null
                && url.getFragment() == null;
    }
}
