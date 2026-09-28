package com.mazurek.eventOrganizer.validators;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LookupNameValidatorUnitTest {

    @Test
    void acceptsHumanReadableNamesThatCanBeEncodedAsPathSegments() {
        assertThatCode(() -> LookupNameValidator.requireValid("New York", 3, 30, "City name"))
                .doesNotThrowAnyException();
        assertThatCode(() -> LookupNameValidator.requireValid("Bielsko-Biała", 3, 30, "City name"))
                .doesNotThrowAnyException();
        assertThatCode(() -> LookupNameValidator.requireValid("O'Fallon", 3, 30, "City name"))
                .doesNotThrowAnyException();
        assertThatCode(() -> LookupNameValidator.requireValid("web-dev", 2, 30, "Tag name"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsRouteDelimitersAndAmbiguousNames() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LookupNameValidator.requireValid("New/York", 3, 30, "City name"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LookupNameValidator.requireValid("topic?sort=asc", 2, 30, "Tag name"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LookupNameValidator.requireValid("city#centre", 3, 30, "City name"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LookupNameValidator.requireValid("New  York", 3, 30, "City name"));
    }
}
