package com.mazurek.eventOrganizer.city;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("City Model Unit Test:")
class CityModelUnitTest {
    @ParameterizedTest(name = "[{index}] timeZoneId={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Invalid/Zone", "+02:00", "UTC+02:00", "Z"})
    void whenTimeZoneIsMissingInvalidOrOffsetOnlyShouldRejectCity(String timeZoneId) {
        assertThatIllegalArgumentException().isThrownBy(() -> new City("place", "City", "PL", null, 0, 0, timeZoneId));
    }

    @Test
    void whenTimeZoneIsValidShouldPreserveIanaIdentifierAndRejectOversizedValue() {
        City city = new City("place", "New York", "US", null, 40, -74, " America/New_York ");
        assertThat(city.getTimeZoneId()).isEqualTo("America/New_York");
        assertThatIllegalArgumentException().isThrownBy(() -> new City("place", "City", "PL", null, 0, 0, "x".repeat(256)));
    }

    @Test
    void whenConstructingCityShouldPreserveNameCaseAndNormalizeCountryCodeAndWhitespace() {
        City city = new City(" PlaceId ", " New York ", " us ", " New York State ", 40, -74, "Europe/Warsaw");
        assertThat(city.getExternalId()).isEqualTo("PlaceId");
        assertThat(city.getName()).isEqualTo("New York");
        assertThat(city.getCountryCode()).isEqualTo("US");
        assertThat(city.getAdminArea()).isEqualTo("New York State");
        assertThat(city.getLatitude()).isEqualTo(40);
        assertThat(city.getLongitude()).isEqualTo(-74);
        assertThat(city.getTimeZoneId()).isEqualTo("Europe/Warsaw");
        assertThat(city.getId()).isNull();
    }

    @ParameterizedTest(name = "[{index}] adminArea={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void whenRegionIsBlankAndCoordinatesAreZeroShouldAcceptCity(String adminArea) {
        City city = new City("place", "City", "GB", adminArea, 0, 0, "Europe/Warsaw");
        assertThat(city.getAdminArea()).isNull();
        assertThat(city.getLatitude()).isZero();
        assertThat(city.getLongitude()).isZero();
    }

    @Test
    void whenFieldsReachValidBoundariesShouldAcceptCity() {
        City city = new City("x".repeat(255), "N".repeat(255), "PL", "A".repeat(255), 90, 180, "Europe/Warsaw");
        assertThat(city.getLatitude()).isEqualTo(90);
        assertThat(city.getLongitude()).isEqualTo(180);
        assertThat(city.getExternalId()).isEqualTo("x".repeat(255));
        assertThat(city.getName()).isEqualTo("N".repeat(255));
        assertThat(city.getAdminArea()).isEqualTo("A".repeat(255));
        City lowerBoundary = new City("place", "City", "PL", null, -90, -180, "Europe/Warsaw");
        assertThat(lowerBoundary.getLatitude()).isEqualTo(-90);
        assertThat(lowerBoundary.getLongitude()).isEqualTo(-180);
    }

    @Test
    void whenProviderFieldsAreOversizedShouldRejectCity() {
        assertThatIllegalArgumentException().isThrownBy(() -> new City("x".repeat(256), "City", "PL", null, 0, 0, "Europe/Warsaw"));
        assertThatIllegalArgumentException().isThrownBy(() -> new City("place", "N".repeat(256), "PL", null, 0, 0, "Europe/Warsaw"));
        assertThatIllegalArgumentException().isThrownBy(() -> new City("place", "City", "PL", "A".repeat(256), 0, 0, "Europe/Warsaw"));
    }
}
