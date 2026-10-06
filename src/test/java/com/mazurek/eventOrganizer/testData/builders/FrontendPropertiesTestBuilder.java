package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import java.net.URI;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class FrontendPropertiesTestBuilder {
    private URI url = PropertyFixtureConstants.FRONTEND_URL;

    public FrontendPropertiesTestBuilder url(URI url) {
        this.url = url;
        return this;
    }

    public static FrontendProperties copyOf(FrontendProperties source) {
        if (source == null) return null;
        return new FrontendPropertiesTestBuilder()
                .url(source.getUrl())
                .build();
    }


    public FrontendProperties build() {
        FrontendProperties value = new FrontendProperties();
        value.setUrl(url);
        return value;
    }
}
