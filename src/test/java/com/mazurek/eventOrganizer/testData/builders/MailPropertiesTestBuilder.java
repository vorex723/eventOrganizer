package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.MailProperties;

import static com.mazurek.eventOrganizer.testData.TestConstants.PropertyFixtureConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class MailPropertiesTestBuilder {
    private String activationBaseUrl = PropertyFixtureConstants.ACTIVATION_BASE_URL;
    private String passwordResetBaseUrl = PropertyFixtureConstants.PASSWORD_RESET_BASE_URL;
    private String emailChangeBaseUrl = PropertyFixtureConstants.EMAIL_CHANGE_BASE_URL;
    private String fromAddress = PropertyFixtureConstants.FROM_ADDRESS;

    public MailPropertiesTestBuilder activationBaseUrl(String activationBaseUrl) {
        this.activationBaseUrl = activationBaseUrl;
        return this;
    }

    public MailPropertiesTestBuilder passwordResetBaseUrl(String passwordResetBaseUrl) {
        this.passwordResetBaseUrl = passwordResetBaseUrl;
        return this;
    }

    public MailPropertiesTestBuilder emailChangeBaseUrl(String emailChangeBaseUrl) {
        this.emailChangeBaseUrl = emailChangeBaseUrl;
        return this;
    }

    public MailPropertiesTestBuilder fromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
        return this;
    }

    public static MailProperties copyOf(MailProperties source) {
        if (source == null) return null;
        return new MailPropertiesTestBuilder()
                .activationBaseUrl(source.getActivationBaseUrl())
                .passwordResetBaseUrl(source.getPasswordResetBaseUrl())
                .emailChangeBaseUrl(source.getEmailChangeBaseUrl())
                .fromAddress(source.getFromAddress())
                .build();
    }


    public MailProperties build() {
        MailProperties value = new MailProperties();
        value.setActivationBaseUrl(activationBaseUrl);
        value.setPasswordResetBaseUrl(passwordResetBaseUrl);
        value.setEmailChangeBaseUrl(emailChangeBaseUrl);
        value.setFromAddress(fromAddress);
        return value;
    }
}
