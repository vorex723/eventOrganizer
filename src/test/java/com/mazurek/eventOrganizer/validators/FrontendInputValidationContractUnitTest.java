package com.mazurek.eventOrganizer.validators;

import com.mazurek.eventOrganizer.testData.builders.AuthenticationRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ResetPasswordRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.EmailBasedRequestTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FrontendInputValidationContractUnitTest contracts:")
class FrontendInputValidationContractUnitTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void whenEmailHasLongTldShouldAcceptItAcrossInputFlows() {
        String email = "person@example.technology";

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .email(email)
                .emailConfirmation(email)
                .build())).isEmpty();
        assertThat(validator.validate(new AuthenticationRequestTestBuilder()
                .email(email)
                .password(com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.USER_PASSWORD)
                .build())).isEmpty();
        assertThat(validator.validate(new EmailBasedRequestTestBuilder()
                .email(email)
                .build())).isEmpty();
        assertThat(validator.validate(ChangeUserEmailDtoTestBuilder.validChange()
                .newEmail(email)
                .newEmailConfirmation(email)
                .build())).isEmpty();
    }

    @Test
    void whenEmailReachesLengthBoundaryShouldAcceptLimitAndRejectOverflow() {
        String emailAtLimit = "a".repeat(64) + "@" + "b".repeat(63) + "."
                + "c".repeat(63) + "." + "d".repeat(62);
        String emailOverLimit = "a".repeat(64) + "@" + "b".repeat(63) + "."
                + "c".repeat(63) + "." + "d".repeat(63);

        assertThat(emailAtLimit).hasSize(ValidationConstraints.EMAIL_MAX_LENGTH);
        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .email(emailAtLimit)
                .emailConfirmation(emailAtLimit)
                .build())).isEmpty();

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .email(emailOverLimit)
                .emailConfirmation(emailOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email", "emailConfirmation");
    }

    @Test
    void whenPasswordReachesValidBoundariesShouldAcceptAcrossInputFlows() {
        String minimumPassword = "Aa1!aaaa";
        String maximumPassword = "Aa1!" + "a".repeat(28);

        assertThat(minimumPassword).hasSize(ValidationConstraints.PASSWORD_MIN_LENGTH);
        assertThat(maximumPassword).hasSize(ValidationConstraints.PASSWORD_MAX_LENGTH);

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .password(minimumPassword)
                .passwordConfirmation(minimumPassword)
                .build())).isEmpty();
        assertThat(validator.validate(new ResetPasswordRequestTestBuilder()
                .password(maximumPassword)
                .passwordConfirmation(maximumPassword)
                .build())).isEmpty();
        assertThat(validator.validate(ChangeUserPasswordDtoTestBuilder.validChange()
                .newPassword(maximumPassword)
                .newPasswordConfirmation(maximumPassword)
                .build())).isEmpty();
    }

    @Test
    void whenPasswordExceedsLimitShouldRejectAcrossInputFlows() {
        String passwordOverLimit = "Aa1!" + "a".repeat(29);

        assertThat(passwordOverLimit).hasSize(ValidationConstraints.PASSWORD_MAX_LENGTH + 1);
        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .password(passwordOverLimit)
                .passwordConfirmation(passwordOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password", "passwordConfirmation");
        assertThat(validator.validate(new ResetPasswordRequestTestBuilder()
                .password(passwordOverLimit)
                .passwordConfirmation(passwordOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password", "passwordConfirmation");
        assertThat(validator.validate(ChangeUserPasswordDtoTestBuilder.validChange()
                .newPassword(passwordOverLimit)
                .newPasswordConfirmation(passwordOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("newPassword", "newPasswordConfirmation");
    }

    @Test
    void whenCityIdentifierReachesBoundaryShouldAlignAccountAndEventValidation() {
        String cityAtLimit = "test:" + "C".repeat(250);
        String cityOverLimit = cityAtLimit + "C";

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .homeCityExternalId(cityAtLimit)
                .build())).isEmpty();
        assertThat(validator.validate(ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCityExternalId(cityAtLimit)
                .build())).isEmpty();

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .homeCityExternalId(cityOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("homeCityExternalId");
        assertThat(validator.validate(ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCityExternalId(cityOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("homeCityExternalId");
    }

    @Test
    void whenRegisteringWithoutTimeZoneShouldAllowItWhileValidatingProfilePreference() {
        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest().build())).isEmpty();
        assertThat(validator.validate(ChangeUserDetailsDtoTestBuilder.validUpdate().timeZone("Invalid/Zone").build()))
                .extracting(violation -> violation.getPropertyPath().toString()).contains("timeZone");
    }
}
