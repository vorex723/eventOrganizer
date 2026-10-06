package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.config.properties.AuthProperties.Email;
import com.mazurek.eventOrganizer.config.properties.AuthProperties.RateLimit;

import static com.mazurek.eventOrganizer.testData.TestConstants.ActivationTokenConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class AuthPropertiesTestBuilder {
    private long activationTokenExpiration = ActivationTokenConstants.ACTIVATION_TOKEN_EXPIRATION_MILLIS;
    private long passwordResetTokenExpiration = PropertyFixtureConstants.PASSWORD_RESET_EXPIRATION;
    private long emailChangeTokenExpiration = PropertyFixtureConstants.EMAIL_CHANGE_EXPIRATION;
    private Email email;
    private boolean emailSet;
    private RateLimit rateLimit;
    private boolean rateLimitSet;

    public AuthPropertiesTestBuilder activationTokenExpiration(long activationTokenExpiration) {
        this.activationTokenExpiration = activationTokenExpiration;
        return this;
    }

    public AuthPropertiesTestBuilder passwordResetTokenExpiration(long passwordResetTokenExpiration) {
        this.passwordResetTokenExpiration = passwordResetTokenExpiration;
        return this;
    }

    public AuthPropertiesTestBuilder emailChangeTokenExpiration(long emailChangeTokenExpiration) {
        this.emailChangeTokenExpiration = emailChangeTokenExpiration;
        return this;
    }

    public AuthPropertiesTestBuilder email(Email email) {
        this.email = AuthEmailPropertiesTestBuilder.copyOf(email);
        this.emailSet = true;
        return this;
    }

    public AuthPropertiesTestBuilder rateLimit(RateLimit rateLimit) {
        this.rateLimit = AuthRateLimitPropertiesTestBuilder.copyOf(rateLimit);
        this.rateLimitSet = true;
        return this;
    }

    public static AuthProperties copyOf(AuthProperties source) {
        if (source == null) return null;
        return new AuthPropertiesTestBuilder()
                .activationTokenExpiration(source.getActivationTokenExpiration())
                .passwordResetTokenExpiration(source.getPasswordResetTokenExpiration())
                .emailChangeTokenExpiration(source.getEmailChangeTokenExpiration())
                .email(source.getEmail())
                .rateLimit(source.getRateLimit())
                .build();
    }


    public AuthProperties build() {
        AuthProperties value = new AuthProperties();
        value.setActivationTokenExpiration(activationTokenExpiration);
        value.setPasswordResetTokenExpiration(passwordResetTokenExpiration);
        value.setEmailChangeTokenExpiration(emailChangeTokenExpiration);
        value.setEmail(emailSet ? AuthEmailPropertiesTestBuilder.copyOf(email) : new AuthEmailPropertiesTestBuilder().build());
        value.setRateLimit(rateLimitSet ? AuthRateLimitPropertiesTestBuilder.copyOf(rateLimit) : new AuthRateLimitPropertiesTestBuilder().build());
        return value;
    }
}
