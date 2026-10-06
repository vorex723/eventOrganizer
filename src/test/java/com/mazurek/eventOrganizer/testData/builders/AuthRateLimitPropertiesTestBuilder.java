package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.AuthProperties.RateLimit;
import java.time.Duration;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class AuthRateLimitPropertiesTestBuilder {
    private boolean enabled = PropertyFixtureConstants.ENABLED;
    private boolean trustForwardedHeaders = PropertyFixtureConstants.DISABLED;
    private String keySecret = PropertyFixtureConstants.RATE_LIMIT_SECRET;
    private int registrationMaxRequests = PropertyFixtureConstants.REGISTRATION_MAX_REQUESTS;
    private Duration registrationWindow = PropertyFixtureConstants.REGISTRATION_WINDOW;
    private int loginMaxRequests = PropertyFixtureConstants.LOGIN_MAX_REQUESTS;
    private Duration loginWindow = PropertyFixtureConstants.LOGIN_WINDOW;

    public AuthRateLimitPropertiesTestBuilder enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AuthRateLimitPropertiesTestBuilder trustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
        return this;
    }

    public AuthRateLimitPropertiesTestBuilder keySecret(String keySecret) {
        this.keySecret = keySecret;
        return this;
    }

    public AuthRateLimitPropertiesTestBuilder registrationMaxRequests(int registrationMaxRequests) {
        this.registrationMaxRequests = registrationMaxRequests;
        return this;
    }

    public AuthRateLimitPropertiesTestBuilder registrationWindow(Duration registrationWindow) {
        this.registrationWindow = registrationWindow;
        return this;
    }

    public AuthRateLimitPropertiesTestBuilder loginMaxRequests(int loginMaxRequests) {
        this.loginMaxRequests = loginMaxRequests;
        return this;
    }

    public AuthRateLimitPropertiesTestBuilder loginWindow(Duration loginWindow) {
        this.loginWindow = loginWindow;
        return this;
    }

    public static RateLimit copyOf(RateLimit source) {
        if (source == null) return null;
        return new AuthRateLimitPropertiesTestBuilder()
                .enabled(source.isEnabled())
                .trustForwardedHeaders(source.isTrustForwardedHeaders())
                .keySecret(source.getKeySecret())
                .registrationMaxRequests(source.getRegistrationMaxRequests())
                .registrationWindow(source.getRegistrationWindow())
                .loginMaxRequests(source.getLoginMaxRequests())
                .loginWindow(source.getLoginWindow())
                .build();
    }


    public RateLimit build() {
        RateLimit value = new RateLimit();
        value.setEnabled(enabled);
        value.setTrustForwardedHeaders(trustForwardedHeaders);
        value.setKeySecret(keySecret);
        value.setRegistrationMaxRequests(registrationMaxRequests);
        value.setRegistrationWindow(registrationWindow);
        value.setLoginMaxRequests(loginMaxRequests);
        value.setLoginWindow(loginWindow);
        return value;
    }
}
