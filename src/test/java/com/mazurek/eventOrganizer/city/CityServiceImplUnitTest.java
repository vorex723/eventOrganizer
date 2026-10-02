package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.*;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceImplUnitTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Invalid/Zone", "UTC+02:00", "+02:00"})
    void rejectsProviderTimeZoneWithoutPersistingCity(String timeZoneId) {
        when(client.getById("place")).thenReturn(new ResolvedCity("place", "City", "PL", null, null, 0, 0, timeZoneId));
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @Mock private CityRepository repository;
    @Mock private CityLookupClient client;
    @InjectMocks private CityServiceImpl service;

    @Test
    void existingCityIsReadLocallyWithoutProviderOrInsert() {
        City city = CityTestBuilder.warsaw().build();
        when(repository.findByExternalId(city.getExternalId())).thenReturn(Optional.of(city));
        assertThat(service.resolve("  " + city.getExternalId() + "  ")).isSameAs(city);
        verifyNoInteractions(client);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    void newCityUsesCompleteProviderDataAndSubsequentResolveUsesDatabase() {
        City stored = CityTestBuilder.warsaw().name("Warsaw").externalId("place").id(UUID.randomUUID()).build();
        when(repository.findByExternalId("place")).thenReturn(Optional.empty(), Optional.of(stored));
        when(client.getById("place")).thenReturn(new ResolvedCity("place", "Warsaw", "pl", null, "Test region", 52.2297, 21.0122, "Europe/Warsaw"));
        assertThat(service.resolve("place")).isSameAs(stored);
        assertThat(service.resolve("place")).isSameAs(stored);
        verify(client, times(1)).getById("place");
        verify(repository, times(1)).insertIfAbsent(any(UUID.class), eq("place"), eq("Warsaw"), eq("PL"), eq("Test region"), eq(52.2297), eq(21.0122), eq("Europe/Warsaw"));
    }

    @Test
    void concurrentInsertWinnerIsReturned() {
        City winner = CityTestBuilder.warsaw().build();
        String id = winner.getExternalId();
        when(repository.findByExternalId(id)).thenReturn(Optional.empty(), Optional.of(winner));
        when(client.getById(id)).thenReturn(new ResolvedCity(id, winner.getName(), "PL", null, null, 0, 0, "Europe/Warsaw"));
        when(repository.insertIfAbsent(any(), eq(id), anyString(), anyString(), isNull(), anyDouble(), anyDouble(), anyString())).thenReturn(0);
        assertThat(service.resolve(id)).isSameAs(winner);
    }

    @Test
    void providerFailureDoesNotWriteToDatabase() {
        CityLookupException failure = new CityLookupException("Unavailable");
        when(client.getById("place")).thenThrow(failure);
        assertThatThrownBy(() -> service.resolve("place")).isSameAs(failure);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void invalidInputDoesNotAccessDatabaseOrProvider(String id) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.resolve(id));
        verifyNoInteractions(repository, client);
    }

    @Test
    void oversizedIdentifierDoesNotAccessDatabaseOrProvider() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.resolve("x".repeat(256)));
        verifyNoInteractions(repository, client);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.NEGATIVE_INFINITY, -181, 181})
    void invalidProviderLongitudeIsRejectedWithoutInsert(double longitude) {
        when(client.getById("place")).thenReturn(new ResolvedCity("place", "City", "PL", null, null, 0, longitude, "Europe/Warsaw"));
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void missingProviderNameIsRejectedWithoutInsert(String name) {
        when(client.getById("place")).thenReturn(new ResolvedCity("place", name, "PL", null, null, 0, 0, "Europe/Warsaw"));
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    void identifierIsCaseSensitive() {
        City city = CityTestBuilder.warsaw().externalId("MixedCaseId").build();
        when(repository.findByExternalId("MixedCaseId")).thenReturn(Optional.of(city));
        assertThat(service.resolve("MixedCaseId")).isSameAs(city);
        verify(repository).findByExternalId("MixedCaseId");
    }

    @Test
    void missingProviderResponseIsRejected() {
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @Test
    void mismatchedIdentifierIsRejected() {
        when(client.getById("place")).thenReturn(new ResolvedCity("other", "City", "PL", null, null, 0, 0, "Europe/Warsaw"));
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, -91, 91})
    void invalidProviderCoordinatesAreRejectedWithoutInsert(double latitude) {
        when(client.getById("place")).thenReturn(new ResolvedCity("place", "City", "PL", null, null, latitude, 0, "Europe/Warsaw"));
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
        verify(repository, never()).insertIfAbsent(any(), anyString(), anyString(), anyString(), any(), anyDouble(), anyDouble(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"P", "POL", "12"})
    void invalidProviderCountryCodeIsRejected(String code) {
        when(client.getById("place")).thenReturn(new ResolvedCity("place", "City", code, null, null, 0, 0, "Europe/Warsaw"));
        assertThatThrownBy(() -> service.resolve("place")).isInstanceOf(CityLookupException.class);
    }

    @Test
    void detailsIncludeGeographyAndEventCountWithoutProviderCall() {
        City city = CityTestBuilder.warsaw().build();
        when(repository.findById(city.getId())).thenReturn(Optional.of(city));
        when(repository.countEventsByCityId(city.getId())).thenReturn(3L);
        CityDto result = service.getCityById(city.getId());
        assertThat(result.getExternalId()).isEqualTo(city.getExternalId());
        assertThat(result.getCountryCode()).isEqualTo("PL");
        assertThat(result.getEventCount()).isEqualTo(3);
        verifyNoInteractions(client);
    }

    @Test
    void missingCityUsesDomainNotFoundError() {
        assertThatThrownBy(() -> service.getCityById(UUID.randomUUID())).isInstanceOf(CityNotFoundException.class);
        verifyNoInteractions(client);
    }

    @Test
    void searchDelegatesWithoutPersistence() {
        var results = List.of(new CitySearchResult("place", "Warsaw", "PL", "Poland", null));
        when(client.search("Wars", "PL")).thenReturn(results);
        assertThat(service.search("Wars", "PL")).isSameAs(results);
        verifyNoInteractions(repository);
    }
}
