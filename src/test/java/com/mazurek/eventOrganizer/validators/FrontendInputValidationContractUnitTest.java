package com.mazurek.eventOrganizer.validators;

import com.mazurek.eventOrganizer.auth.dto.AuthenticationRequest;
import com.mazurek.eventOrganizer.auth.dto.EmailBasedRequest;
import com.mazurek.eventOrganizer.auth.dto.ResetPasswordRequest;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserDetailsDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserPasswordDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.RegisterRequestTestBuilder;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    void acceptsLongTldsAcrossEveryEmailInput() {
        String email = "person@example.technology";

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .email(email)
                .emailConfirmation(email)
                .build())).isEmpty();
        assertThat(validator.validate(AuthenticationRequest.builder()
                .email(email)
                .password("Valid1!Password")
                .build())).isEmpty();
        assertThat(validator.validate(new EmailBasedRequest(email))).isEmpty();
        assertThat(validator.validate(ChangeUserEmailDtoTestBuilder.validChange()
                .newEmail(email)
                .newEmailConfirmation(email)
                .build())).isEmpty();
    }

    @Test
    void acceptsAnEmailAtThe255CharacterLimitAndRejectsOneBeyondIt() {
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
    void acceptsMinimumAndMaximumPasswordsAcrossEveryNewPasswordFlow() {
        String minimumPassword = "Aa1!aaaa";
        String maximumPassword = "Aa1!" + "a".repeat(28);

        assertThat(minimumPassword).hasSize(ValidationConstraints.PASSWORD_MIN_LENGTH);
        assertThat(maximumPassword).hasSize(ValidationConstraints.PASSWORD_MAX_LENGTH);

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .password(minimumPassword)
                .passwordConfirmation(minimumPassword)
                .build())).isEmpty();
        assertThat(validator.validate(new ResetPasswordRequest(maximumPassword, maximumPassword))).isEmpty();
        assertThat(validator.validate(ChangeUserPasswordDtoTestBuilder.validChange()
                .newPassword(maximumPassword)
                .newPasswordConfirmation(maximumPassword)
                .build())).isEmpty();
    }

    @Test
    void rejectsA33CharacterPasswordAcrossEveryNewPasswordFlow() {
        String passwordOverLimit = "Aa1!" + "a".repeat(29);

        assertThat(passwordOverLimit).hasSize(ValidationConstraints.PASSWORD_MAX_LENGTH + 1);
        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .password(passwordOverLimit)
                .passwordConfirmation(passwordOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("password", "passwordConfirmation");
        assertThat(validator.validate(new ResetPasswordRequest(passwordOverLimit, passwordOverLimit)))
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
    void keepsAccountCityValidationAlignedWithEventCityValidation() {
        String cityAtLimit = "C".repeat(ValidationConstraints.CITY_MAX_LENGTH);
        String cityOverLimit = cityAtLimit + "C";

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .homeCity(cityAtLimit)
                .build())).isEmpty();
        assertThat(validator.validate(ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCity(cityAtLimit)
                .build())).isEmpty();

        assertThat(validator.validate(RegisterRequestTestBuilder.firstUserRegisterRequest()
                .homeCity(cityOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("homeCity");
        assertThat(validator.validate(ChangeUserDetailsDtoTestBuilder.validUpdate()
                .homeCity(cityOverLimit)
                .build()))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("homeCity");
    }
}
