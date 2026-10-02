package com.mazurek.eventOrganizer.city;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class CityModelUnitTest {
    @Test
    void preservesDisplayCaseAndNormalizesCountryCodeAndWhitespace() {
        City city = new City(" PlaceId ", " New York ", " us ", " New York State ", 40, -74);
        assertThat(city.getExternalId()).isEqualTo("PlaceId");
        assertThat(city.getName()).isEqualTo("New York");
        assertThat(city.getCountryCode()).isEqualTo("US");
        assertThat(city.getAdminArea()).isEqualTo("New York State");
        assertThat(city.getId()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void optionalRegionAndZeroCoordinatesAreValid(String adminArea) {
        City city = new City("place", "City", "GB", adminArea, 0, 0);
        assertThat(city.getAdminArea()).isNull();
        assertThat(city.getLatitude()).isZero();
        assertThat(city.getLongitude()).isZero();
    }

    @Test
    void acceptsCoordinateBoundariesAndMaximumTextLengths() {
        City city = new City("x".repeat(255), "N".repeat(255), "PL", "A".repeat(255), 90, 180);
        assertThat(city.getLatitude()).isEqualTo(90);
        assertThat(city.getLongitude()).isEqualTo(180);
        assertThat(new City("place", "City", "PL", null, -90, -180).getLatitude()).isEqualTo(-90);
    }

    @Test
    void rejectsOversizedProviderFields() {
        assertThatIllegalArgumentException().isThrownBy(() -> new City("x".repeat(256), "City", "PL", null, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new City("place", "N".repeat(256), "PL", null, 0, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new City("place", "City", "PL", "A".repeat(256), 0, 0));
    }
}
