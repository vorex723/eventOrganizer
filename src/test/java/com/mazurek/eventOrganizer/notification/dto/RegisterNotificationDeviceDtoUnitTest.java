package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RegisterNotificationDeviceDto unit tests:")
class RegisterNotificationDeviceDtoUnitTest {

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

    @Nested
    @DisplayName("Firebase installation ID validation tests:")
    class FirebaseInstallationIdValidationTests {

        @Test
        @DisplayName("When ID has 255 characters should be valid")
        void whenIdHas255CharactersShouldBeValid() {
            RegisterNotificationDeviceDto request = requestWithInstallationId("a".repeat(255));

            assertThat(validator.validate(request)).isEmpty();
        }

        @Test
        @DisplayName("When ID exceeds 255 characters should be invalid")
        void whenIdExceeds255CharactersShouldBeInvalid() {
            RegisterNotificationDeviceDto request = requestWithInstallationId("a".repeat(256));

            Set<ConstraintViolation<RegisterNotificationDeviceDto>> violations = validator.validate(request);

            assertThat(violations)
                    .singleElement()
                    .satisfies(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("firebaseInstallationId");
                        assertThat(violation.getMessage())
                                .isEqualTo("Firebase installation id must not exceed 255 characters.");
                    });
        }

        @ParameterizedTest
        @ValueSource(strings = {" firebase-installation-id", "firebase-installation-id "})
        @DisplayName("When ID has surrounding whitespace should be invalid")
        void whenIdHasSurroundingWhitespaceShouldBeInvalid(String firebaseInstallationId) {
            Set<ConstraintViolation<RegisterNotificationDeviceDto>> violations = validator.validate(
                    requestWithInstallationId(firebaseInstallationId)
            );

            assertThat(violations)
                    .singleElement()
                    .satisfies(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("firebaseInstallationId");
                        assertThat(violation.getMessage())
                                .isEqualTo("Firebase installation id must not contain surrounding whitespace.");
                    });
        }

        @Test
        @DisplayName("When platform is null should be invalid")
        void whenPlatformIsNullShouldBeInvalid() {
            Set<ConstraintViolation<RegisterNotificationDeviceDto>> violations = validator.validate(
                    new RegisterNotificationDeviceDto(null, "firebase-installation-id")
            );

            assertThat(violations)
                    .singleElement()
                    .satisfies(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("platform");
                        assertThat(violation.getMessage()).isEqualTo("Device platform must be provided.");
                    });
        }

        @Test
        @DisplayName("When ID is null should be invalid")
        void whenIdIsNullShouldBeInvalid() {
            Set<ConstraintViolation<RegisterNotificationDeviceDto>> violations = validator.validate(
                    requestWithInstallationId(null)
            );

            assertThat(violations)
                    .singleElement()
                    .satisfies(violation -> {
                        assertThat(violation.getPropertyPath().toString()).isEqualTo("firebaseInstallationId");
                        assertThat(violation.getMessage()).isEqualTo("Firebase installation id must be provided.");
                    });
        }

        private RegisterNotificationDeviceDto requestWithInstallationId(String firebaseInstallationId) {
            return new RegisterNotificationDeviceDto(DevicePlatform.ANDROID, firebaseInstallationId);
        }
    }
}
