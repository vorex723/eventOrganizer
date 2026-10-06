package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.ApiCorsProperties;
import java.util.ArrayList;
import java.util.List;



/** Constructs data only; explicit overrides are passed through without repair. */
public class ApiCorsPropertiesTestBuilder {
    private List<String> allowedOrigins = List.of();

    public ApiCorsPropertiesTestBuilder allowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? null : new ArrayList<>(allowedOrigins);
        return this;
    }

    public static ApiCorsProperties copyOf(ApiCorsProperties source) {
        if (source == null) return null;
        return new ApiCorsPropertiesTestBuilder()
                .allowedOrigins(source.getAllowedOrigins())
                .build();
    }


    public ApiCorsProperties build() {
        ApiCorsProperties value = new ApiCorsProperties();
        value.setAllowedOrigins(allowedOrigins == null ? null : new ArrayList<>(allowedOrigins));
        return value;
    }
}
