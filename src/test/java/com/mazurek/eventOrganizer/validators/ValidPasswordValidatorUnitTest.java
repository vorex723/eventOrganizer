package com.mazurek.eventOrganizer.validators;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ValidPasswordValidatorUnitTest {
    private final ValidPasswordValidator validator = new ValidPasswordValidator();

    @ParameterizedTest
    @ValueSource(strings = {"Valid1!Password", "Valid1@Password", "Valid1=Password"})
    void acceptsEveryDocumentedSpecialCharacter(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"weakpassword", "NoSpecial1", "NoNumber!", "Valid 1!Password"})
    void rejectsPasswordsOutsideTheSharedPolicy(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }
}
