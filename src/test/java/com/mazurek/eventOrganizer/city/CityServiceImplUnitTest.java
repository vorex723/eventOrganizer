package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.*;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.testData.builders.CitySearchResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ResolvedCityTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.CityLookupConstants.*;
import static com.mazurek.eventOrganizer.testData.TestConstants.CitiesConstants.WARSAW_ID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("City Service Impl Unit Test:")
class CityServiceImplUnitTest {
    @Mock private CityRepository repository;
    @Mock private CityLookupClient client;
    @InjectMocks private CityServiceImpl service;

    @ParameterizedTest(name = "[{index}] timeZoneId={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Invalid/Zone", "UTC+02:00", "+02:00"})
    void whenProviderTimeZoneIsInvalidShouldRejectCityWithoutInsert(String timeZoneId) {
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("place")
                .timeZoneId(timeZoneId)
                .build());
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @Test
    void whenCityExistsShouldReadLocallyWithoutProviderOrInsert() {
        City city = CityTestBuilder.warsaw().build();
        when(repository.findByExternalId(city.getExternalId())).thenReturn(Optional.of(city));
        assertThat(service.resolve("  " + city.getExternalId() + "  ")).isSameAs(city);
        verifyNoInteractions(client);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @Test
    void whenResolvingNewCityShouldInsertProviderDataAndReadLocallyOnSubsequentResolve() {
        City stored = CityTestBuilder.warsaw().name(NAME).externalId("place").build();
        when(repository.findByExternalId("place")).thenReturn(Optional.empty(), Optional.of(stored));
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("place")
                .countryCode("pl")
                .adminArea("Test region")
                .build());
        assertThat(service.resolve("place")).isSameAs(stored);
        assertThat(service.resolve("place")).isSameAs(stored);
        verify(client, times(1)).getById("place");
        verify(repository, times(1)).insertIfAbsent(any(UUID.class), eq("place"), eq(NAME), eq(COUNTRY_CODE),
                eq("Test region"), eq(LATITUDE), eq(LONGITUDE), eq(TIME_ZONE_ID));
        verify(repository, times(3)).findByExternalId("place");
    }

    @Test
    void whenConcurrentInsertWinsShouldReturnPersistedWinner() {
        City winner = CityTestBuilder.warsaw().build();
        String id = winner.getExternalId();
        when(repository.findByExternalId(id)).thenReturn(Optional.empty(), Optional.of(winner));
        when(client.getById(id)).thenReturn(new ResolvedCityTestBuilder()
                .externalId(id)
                .name("Provider losing candidate")
                .countryCode("PL")
                .countryName(null)
                .adminArea(null)
                .latitude(0)
                .longitude(0)
                .timeZoneId("Europe/Warsaw")
                .build());
        when(repository.insertIfAbsent(any(), eq(id), anyString(), anyString(), isNull(), anyDouble(), anyDouble(), anyString())).thenReturn(0);
        assertThat(service.resolve(id)).isSameAs(winner);
        verify(repository).insertIfAbsent(any(UUID.class), eq(id), eq("Provider losing candidate"),
                eq("PL"), isNull(), eq(0.0), eq(0.0), eq("Europe/Warsaw"));
        verify(repository, times(2)).findByExternalId(id);
        verify(client).getById(id);
    }

    @Test
    void whenProviderFailsShouldPropagateFailureWithoutInsert() {
        CityLookupException failure = new CityLookupException("Unavailable");
        when(client.getById("place")).thenThrow(failure);
        assertThatThrownBy(() -> service.resolve("place")).isSameAs(failure);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @ParameterizedTest(name = "[{index}] id={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void whenIdentifierIsBlankShouldRejectWithoutAccessingDatabaseOrProvider(String id) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.resolve(id));
        verifyNoInteractions(repository, client);
    }

    @Test
    void whenIdentifierIsOversizedShouldRejectWithoutAccessingDatabaseOrProvider() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.resolve("x".repeat(256)));
        verifyNoInteractions(repository, client);
    }

    @ParameterizedTest(name = "[{index}] longitude={0}")
    @ValueSource(doubles = {Double.NaN, Double.NEGATIVE_INFINITY, -181, 181})
    void whenProviderLongitudeIsInvalidShouldRejectWithoutInsert(double longitude) {
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("place")
                .longitude(longitude)
                .build());
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @ParameterizedTest(name = "[{index}] name={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void whenProviderNameIsMissingShouldRejectWithoutInsert(String name) {
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("place")
                .name(name)
                .build());
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @Test
    void whenIdentifierHasMixedCaseShouldPreserveCase() {
        City city = CityTestBuilder.warsaw().externalId("MixedCaseId").build();
        when(repository.findByExternalId("MixedCaseId")).thenReturn(Optional.of(city));
        assertThat(service.resolve("MixedCaseId")).isSameAs(city);
        verify(repository).findByExternalId("MixedCaseId");
    }

    @Test
    void whenProviderResponseIsMissingShouldRejectWithoutInsert() {
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @Test
    void whenProviderIdentifierDoesNotMatchShouldRejectWithoutInsert() {
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("other")
                .build());
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @ParameterizedTest(name = "[{index}] latitude={0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, -91, 91})
    void whenProviderLatitudeIsInvalidShouldRejectWithoutInsert(double latitude) {
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("place")
                .latitude(latitude)
                .build());
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @ParameterizedTest(name = "[{index}] code={0}")
    @NullAndEmptySource
    @ValueSource(strings = {"P", "POL", "12"})
    void whenProviderCountryCodeIsInvalidShouldRejectWithoutInsert(String code) {
        when(client.getById("place")).thenReturn(new ResolvedCityTestBuilder()
                .externalId("place")
                .countryCode(code)
                .build());
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), any(), any(), any(), any(), anyDouble(), anyDouble(), any());
    }

    @Test
    void whenReadingCityDetailsShouldIncludeGeographyAndEventCountWithoutProviderCall() {
        City city = CityTestBuilder.warsaw().build();
        when(repository.findById(city.getId())).thenReturn(Optional.of(city));
        when(repository.countEventsByCityId(city.getId())).thenReturn(3L);
        CityDto result = service.getCityById(city.getId());
        assertThat(result).isNotNull();
        assertThat(result).extracting(CityDto::getId, CityDto::getExternalId, CityDto::getName,
                        CityDto::getCountryCode, CityDto::getAdminArea, CityDto::getLatitude,
                        CityDto::getLongitude, CityDto::getEventCount)
                .containsExactly(city.getId(), city.getExternalId(), city.getName(), city.getCountryCode(),
                        city.getAdminArea(), city.getLatitude(), city.getLongitude(), 3L);
        verifyNoInteractions(client);
    }

    @Test
    void whenCityIsMissingShouldThrowDomainNotFoundWithoutProviderCall() {
        assertThatThrownBy(() -> service.getCityById(WARSAW_ID)).isInstanceOf(CityNotFoundException.class);
        verifyNoInteractions(client);
    }

    @Test
    void whenSearchingShouldDelegateWithoutPersistence() {
        var results = List.of(new CitySearchResultTestBuilder()
                .externalId("place")
                .displayName("Warsaw")
                .countryCode("PL")
                .countryName("Poland")
                .adminArea(null)
                .build());
        when(client.search("Wars", "PL")).thenReturn(results);
        assertThat(service.search("Wars", "PL")).isSameAs(results);
        verifyNoInteractions(repository);
    }
}
