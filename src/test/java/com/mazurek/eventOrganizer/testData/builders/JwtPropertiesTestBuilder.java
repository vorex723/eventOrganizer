package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.JwtProperties;

import static com.mazurek.eventOrganizer.testData.TestConstants.JwtConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class JwtPropertiesTestBuilder {
    private long accessExpiration = JwtConstants.ACCESS_TOKEN_EXPIRATION_30_MINUTES;
    private long refreshShortExpiration = JwtConstants.REFRESH_TOKEN_EXPIRATION_SHORT;
    private long refreshLongExpiration = JwtConstants.REFRESH_TOKEN_EXPIRATION_LONG;
    private String secret = JwtConstants.TEST_SECRET_BASE64;

    public JwtPropertiesTestBuilder accessExpiration(long accessExpiration) {
        this.accessExpiration = accessExpiration;
        return this;
    }

    public JwtPropertiesTestBuilder refreshShortExpiration(long refreshShortExpiration) {
        this.refreshShortExpiration = refreshShortExpiration;
        return this;
    }

    public JwtPropertiesTestBuilder refreshLongExpiration(long refreshLongExpiration) {
        this.refreshLongExpiration = refreshLongExpiration;
        return this;
    }

    public JwtPropertiesTestBuilder secret(String secret) {
        this.secret = secret;
        return this;
    }

    public static JwtProperties copyOf(JwtProperties source) {
        if (source == null) return null;
        return new JwtPropertiesTestBuilder()
                .accessExpiration(source.getAccessExpiration())
                .refreshShortExpiration(source.getRefreshShortExpiration())
                .refreshLongExpiration(source.getRefreshLongExpiration())
                .secret(source.getSecret())
                .build();
    }


    public JwtProperties build() {
        JwtProperties value = new JwtProperties();
        value.setAccessExpiration(accessExpiration);
        value.setRefreshShortExpiration(refreshShortExpiration);
        value.setRefreshLongExpiration(refreshLongExpiration);
        value.setSecret(secret);
        return value;
    }
}
