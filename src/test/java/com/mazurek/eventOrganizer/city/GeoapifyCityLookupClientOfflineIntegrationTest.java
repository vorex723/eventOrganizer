package com.mazurek.eventOrganizer.city;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupException;
import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;
import com.mazurek.eventOrganizer.city.cityLookupClient.GeoapifyCityLookupClient;
import com.mazurek.eventOrganizer.city.cityLookupClient.ResolvedCity;
import com.mazurek.eventOrganizer.config.GeoapifyConfig;
import com.mazurek.eventOrganizer.config.properties.GeoapifyProperties;
import com.mazurek.eventOrganizer.testData.builders.CitySearchResultTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.ResolvedCityTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringJUnitConfig(GeoapifyCityLookupClientOfflineIntegrationTest.AdapterConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Geoapify City Lookup Client Offline Integration Test:")
class GeoapifyCityLookupClientOfflineIntegrationTest {
    private static final String API_KEY = "test-api-key";
    private static final String WARSAW_ID = "51a19119b9b8013540596ac592cdb01d4a40f00101f901cb20050000000000c00208920306576172736177";
    private static final String AUTOCOMPLETE = "/v1/geocode/autocomplete";
    private static final String PLACE_DETAILS = "/v2/place-details";

    @RegisterExtension
    static final WireMockExtension provider = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().usingFilesUnderClasspath("wiremock/geoapify"))
            .build();

    @Autowired private GeoapifyCityLookupClient client;
    @Autowired private GeoapifyProperties properties;
    @Autowired private ObjectMapper mapper;
    @Autowired private ApplicationContext context;

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.geoapify.base-url", provider::baseUrl);
        registry.add("app.geoapify.api-key", () -> API_KEY);
        registry.add("app.geoapify.connect-timeout", () -> 1000);
        registry.add("app.geoapify.read-timeout", () -> 1000);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({GeoapifyConfig.class, GeoapifyCityLookupClient.class})
    @ImportAutoConfiguration({JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
            RestClientAutoConfiguration.class})
    static class AdapterConfiguration {}

    @Test
    void whenLoadingAdapterConfigurationShouldUseLocalHttpWithoutDatabase() {
        assertThat(properties.apiKey()).isEqualTo(API_KEY);
        assertThat(properties.baseUrl()).isEqualTo(provider.baseUrl());
        assertThat(properties.connectTimeout()).isEqualTo(1000);
        assertThat(properties.readTimeout()).isEqualTo(1000);
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
    }

    @Test
    void whenAutocompleteHasCountryBiasShouldMapCitiesInProviderOrder() {
        provider.stubFor(autocomplete("Warszawa", "countrycode:pl")
                .willReturn(jsonFile("autocomplete-warszawa-pl.json")));

        assertThat(client.search("  Warszawa  ", "PL")).containsExactlyElementsOf(List.of(
                new CitySearchResultTestBuilder()
                        .externalId("51a19119b9b8013540596ac592cdb01d4a40f00101f901cb20050000000000c00208920306576172736177")
                        .displayName("Warsaw")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Masovian Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51c5c551b9898a3340596ff9a303ed814a40f00103f901af11818100000000c00208920309576172737a61776b61")
                        .displayName("Warszawka")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Kuyavian-Pomeranian Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51f7f6f8ce7953354059a26deb5d72fe4940f00101f901f926b90000000000c0020892030a576172737ac3b3776b61")
                        .displayName("Warszówka")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Masovian Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("5137058df4fd0d3240597fb7e5c86fe44940f00103f9010800e50100000000c0020892030a576172737ac3b3776b61")
                        .displayName("Warszówka")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Greater Poland Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("515cf7a0b140073440598a157f25a1594a40f00103f901b811818100000000c00208920309576172737a65776b61")
                        .displayName("Warszewka")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Masovian Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51b3f1bb44504a324059419e5dbef5df4940f00103f9011600e50100000000c00208920307576172737a6577")
                        .displayName("Warszew")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Greater Poland Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("5162105839b4a83340591cd3139678144b40f00103f901121ce10100000000c00208920308576172737a65776f")
                        .displayName("Warszewo")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Warmian-Masurian Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("5160f767507e3e334059e9a518d643174a40f00101f9018816570000000000c0020892030757617267617761")
                        .displayName("Wargawa")
                        .countryCode("PL")
                        .countryName("Poland")
                        .adminArea("Łódź Voivodeship")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("5161bb20c77b8853c059e6b56d73be5e4540f00101f90148b0020000000000c00208920306576172736177")
                        .displayName("Warsaw")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("New York")
                        .build()
        ));
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @Test
    void whenAutocompleteHasNoCountryBiasShouldKeepCountriesAndProviderOrder() {
        provider.stubFor(autocomplete("Cambridge", "countrycode:none")
                .willReturn(jsonFile("autocomplete-cambridge-no-bias.json")));

        assertThat(client.search("Cambridge", null)).containsExactlyElementsOf(List.of(
                new CitySearchResultTestBuilder()
                        .externalId("5145ecb886be60be3f598aa658da4e1a4a40f00103f90156fe3f0100000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("GB")
                        .countryName("United Kingdom")
                        .adminArea("England")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("516afb57561a3852c059f956da988d524640f00101f901bd09030000000000c0020892031143616d6272696467652056696c6c616765")
                        .displayName("Cambridge Village")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Vermont")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("516afb57561a3852c059f956da988d524640f00101f90104a7870000000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Vermont")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51696e2af7a7c651c05932642b1ecd2e4540f00101f901b1811d0000000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Massachusetts")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51cc3dc9c2fc1354c0594e67823c16ae4540f00101f90148771f0000000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("CA")
                        .countryName("Canada")
                        .adminArea("Ontario")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("517f3a79ec02ef654059c937802326f242c0f00103f901cba69e0300000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("NZ")
                        .countryName("New Zealand")
                        .adminArea("Waikato")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51c5beae705c4e57c059dd8a0e924fc94640f00101f9017417020000000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Minnesota")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51c0417bf5f10453c0590a3dac81f7484340f00101f9012a08020000000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Maryland")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("51253493b9c36554c0592c1f93d629034440f00101f9017bca020000000000c0020892030943616d627269646765")
                        .displayName("Cambridge")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Ohio")
                        .build(),
                new CitySearchResultTestBuilder()
                        .externalId("518c834bc79c0354c059be6bd097dee64440f00101f90171e1020000000000c0020892031143616d62726964676520537072696e6773")
                        .displayName("Cambridge Springs")
                        .countryCode("US")
                        .countryName("United States")
                        .adminArea("Pennsylvania")
                        .build()
        ));
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] query={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "W", " W "})
    void whenQueryIsShortShouldReturnEmptyListWithoutHttpRequests(String query) {
        assertThat(client.search(query, "PL")).isEmpty();
        assertThat(provider.getAllServeEvents()).isEmpty();
    }

    @Test
    void whenAutocompleteResponseIsEmptyShouldReturnEmptyList() {
        provider.stubFor(autocomplete("Warszawa", "countrycode:pl")
                .willReturn(jsonFile("autocomplete-empty.json")));

        assertThat(client.search("Warszawa", "PL")).isEmpty();
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @Test
    void whenReadingPlaceDetailsShouldMapAllFieldsWithoutRequiringResultType() {
        provider.stubFor(details().willReturn(jsonFile("place-details-warsaw.json")));

        ResolvedCity resolved = client.getById("  " + WARSAW_ID + "  ");
        assertThat(resolved).isEqualTo(new ResolvedCityTestBuilder()
                .externalId(WARSAW_ID)
                .name("Warsaw")
                .countryCode("PL")
                .countryName("Poland")
                .adminArea("Masovian Voivodeship")
                .latitude(52.2319581)
                .longitude(21.0067249)
                .timeZoneId("Europe/Warsaw")
                .build());
        assertThatCode(() -> ZoneId.of(resolved.timeZoneId())).doesNotThrowAnyException();
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] englishName={0}")
    @ValueSource(strings = {"Warsaw", "  Warsaw  "})
    void whenEnglishNameExistsShouldPreferItOverLocalCityAndPlaceNames(String englishName) throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        ObjectNode place = detailsProperties(response);
        ((ObjectNode) place.path("name_international")).put("en", englishName);
        place.put("city", "Warszawa");
        place.put("name", "Warszawa");
        stubJson(details(), response);

        assertThat(client.getById(WARSAW_ID).name()).isEqualTo("Warsaw");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(MissingName.class)
    void whenEnglishNameIsUnavailableShouldFallBackToCity(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        ObjectNode place = detailsProperties(response);
        setMissingName((ObjectNode) place.path("name_international"), "en", missing);
        place.put("city", "  Warsaw  ");
        stubJson(details(), response);

        assertThat(client.getById(WARSAW_ID).name()).isEqualTo("Warsaw");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(value = MissingName.class, names = {"ABSENT", "NULL"})
    void whenInternationalNamesAreUnavailableShouldFallBackToCity(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        setMissingName(detailsProperties(response), "name_international", missing);
        stubJson(details(), response);

        assertThat(client.getById(WARSAW_ID).name()).isEqualTo("Warsaw");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(MissingName.class)
    void whenCityNameIsUnavailableShouldUseLocalNameWithoutChoosingAnotherLanguage(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        ObjectNode place = detailsProperties(response);
        ((ObjectNode) place.path("name_international")).remove("en");
        setMissingName(place, "city", missing);
        place.put("name", "  Łęczna  ");
        stubJson(details(), response);

        assertThat(client.getById(WARSAW_ID).name()).isEqualTo("Łęczna");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(MissingName.class)
    void whenCityNamesAreUnavailableShouldRejectEvenWithFormattedAddress(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        ObjectNode place = detailsProperties(response);
        setMissingName((ObjectNode) place.path("name_international"), "en", missing);
        setMissingName(place, "city", missing);
        setMissingName(place, "name", missing);
        stubJson(details(), response);

        assertThatThrownBy(() -> client.getById(WARSAW_ID))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("without usable city name");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @Test
    void whenAutocompleteHasProviderNameShouldPreferItWithoutTrimming() throws IOException {
        ObjectNode response = (ObjectNode) fixture("autocomplete-warszawa-pl.json");
        ObjectNode place = firstAutocompleteResult(response);
        place.put("name", "  Warszawa  ");
        place.put("city", "Warsaw");
        place.putObject("name_international").put("en", "Warsaw");
        stubJson(autocomplete("Warszawa", "countrycode:pl"), response);

        assertThat(client.search("Warszawa", "PL")).first().extracting(CitySearchResult::displayName).isEqualTo("  Warszawa  ");
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(MissingName.class)
    void whenAutocompleteNameIsUnavailableShouldFallBackToCity(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("autocomplete-warszawa-pl.json");
        ObjectNode place = firstAutocompleteResult(response);
        setMissingName(place, "name", missing);
        place.put("city", "  Warsaw  ");
        stubJson(autocomplete("Warszawa", "countrycode:pl"), response);

        assertThat(client.search("Warszawa", "PL")).first().extracting(CitySearchResult::displayName).isEqualTo("  Warsaw  ");
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(MissingName.class)
    void whenAutocompleteNamesAreUnavailableShouldUseFormattedDisplayLabel(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("autocomplete-warszawa-pl.json");
        ObjectNode place = firstAutocompleteResult(response);
        setMissingName(place, "name", missing);
        setMissingName(place, "city", missing);
        stubJson(autocomplete("Warszawa", "countrycode:pl"), response);

        assertThat(client.search("Warszawa", "PL")).first().extracting(CitySearchResult::displayName).isEqualTo("Warsaw, MZ, Poland");
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] missing={0}")
    @EnumSource(MissingName.class)
    void whenAutocompleteDisplayLabelIsUnavailableShouldRejectResult(MissingName missing) throws IOException {
        ObjectNode response = (ObjectNode) fixture("autocomplete-warszawa-pl.json");
        ObjectNode place = firstAutocompleteResult(response);
        setMissingName(place, "name", missing);
        setMissingName(place, "city", missing);
        setMissingName(place, "formatted", missing);
        stubJson(autocomplete("Warszawa", "countrycode:pl"), response);

        assertThatThrownBy(() -> client.search("Warszawa", "PL"))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("without usable name");
        assertSingleRequest("text", "type", "limit", "lang", "format", "bias", "apiKey");
    }

    @Test
    void whenDetailsFeatureIsMissingShouldRejectResponse() {
        provider.stubFor(details().willReturn(jsonFile("place-details-without-details.json")));

        assertThatThrownBy(() -> client.getById(WARSAW_ID))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("no details");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @Test
    void whenFixtureTimeZoneIsInvalidShouldRejectResponse() {
        provider.stubFor(details().willReturn(jsonFile("place-details-invalid-timezone.json")));

        assertThatThrownBy(() -> client.getById(WARSAW_ID))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("invalid timezone");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "[{index}] timezone={0}")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "Invalid/Zone", "+02:00", "UTC+02:00", "Z"})
    void whenTimeZoneIsMissingInvalidOrOffsetOnlyShouldRejectWithoutFallback(String timezone) throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        ObjectNode place = detailsProperties(response);
        ObjectNode zone = (ObjectNode) place.path("timezone");
        zone.put("name", timezone);
        provider.stubFor(details().willReturn(okJson(mapper.writeValueAsString(response))));

        assertThatThrownBy(() -> client.getById(WARSAW_ID)).isInstanceOf(CityLookupException.class);
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @Test
    void whenTimeZoneObjectIsMissingShouldRejectWithoutFallback() throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        detailsProperties(response).remove("timezone");
        provider.stubFor(details().willReturn(okJson(mapper.writeValueAsString(response))));

        assertThatThrownBy(() -> client.getById(WARSAW_ID))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("without timezone");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @Test
    void whenTimeZoneObjectIsNullShouldRejectWithoutFallback() throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        detailsProperties(response).putNull("timezone");
        provider.stubFor(details().willReturn(okJson(mapper.writeValueAsString(response))));

        assertThatThrownBy(() -> client.getById(WARSAW_ID))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("without timezone");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @Test
    void whenTimeZoneNameIsMissingShouldRejectWithoutFallback() throws IOException {
        ObjectNode response = (ObjectNode) fixture("place-details-warsaw.json");
        JsonNode zone = detailsProperties(response).path("timezone");
        assertThat(zone).as("Place Details fixture timezone").isInstanceOf(ObjectNode.class);
        ((ObjectNode) zone).remove("name");
        provider.stubFor(details().willReturn(okJson(mapper.writeValueAsString(response))));

        assertThatThrownBy(() -> client.getById(WARSAW_ID))
                .isInstanceOf(CityLookupException.class).hasMessageContaining("without timezone");
        assertSingleRequest("id", "features", "lang", "apiKey");
    }

    @ParameterizedTest(name = "{0} wraps HTTP {1}")
    @CsvSource({"AUTOCOMPLETE,500", "PLACE_DETAILS,500", "AUTOCOMPLETE,429", "PLACE_DETAILS,429"})
    void whenProviderReturnsHttpErrorShouldWrapWithoutRetry(Lookup operation, int status) {
        provider.stubFor(request(operation).willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> invoke(operation)).isInstanceOf(CityLookupException.class);
        assertThat(provider.getAllServeEvents()).singleElement()
                .satisfies(event -> assertThat(event.getResponseDefinition().getStatus()).isEqualTo(status));
    }

    @ParameterizedTest(name = "[{index}] operation={0}")
    @EnumSource(Lookup.class)
    @Timeout(10)
    void whenProviderReadTimesOutShouldWrapWithoutRetry(Lookup operation) {
        provider.stubFor(request(operation).willReturn(jsonFile(operation == Lookup.AUTOCOMPLETE
                ? "autocomplete-warszawa-pl.json" : "place-details-warsaw.json").withFixedDelay(2500)));

        assertThatThrownBy(() -> invoke(operation)).isInstanceOf(CityLookupException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        // Observe the whole response-delay window for retries before the next test resets the journal.
        // A disconnected client does not guarantee WireMock records a response-send completion time.
        await().during(Duration.ofMillis(2500)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(provider.getAllServeEvents()).singleElement()
                        .satisfies(event -> assertThat(event.getResponseDefinition().getFixedDelayMilliseconds())
                                .isEqualTo(2500)));
    }

    enum Lookup { AUTOCOMPLETE, PLACE_DETAILS }

    enum MissingName { ABSENT, NULL, EMPTY, BLANK }

    private void setMissingName(ObjectNode object, String field, MissingName missing) {
        switch (missing) {
            case ABSENT -> object.remove(field);
            case NULL -> object.putNull(field);
            case EMPTY -> object.put(field, "");
            case BLANK -> object.put(field, " \t\n ");
        }
    }

    private ObjectNode detailsProperties(ObjectNode response) {
        JsonNode features = response.path("features");
        assertThat(features.isArray()).as("Place Details fixture features array").isTrue();
        assertThat(features.size()).as("Place Details fixture must contain a feature").isPositive();
        JsonNode properties = features.path(0).path("properties");
        assertThat(properties).as("Place Details fixture properties").isInstanceOf(ObjectNode.class);
        return (ObjectNode) properties;
    }

    private ObjectNode firstAutocompleteResult(ObjectNode response) {
        JsonNode results = response.path("results");
        assertThat(results.isArray()).as("Autocomplete fixture results array").isTrue();
        assertThat(results.size()).as("Autocomplete fixture must contain a result").isPositive();
        assertThat(results.path(0)).as("First autocomplete fixture result").isInstanceOf(ObjectNode.class);
        return (ObjectNode) results.path(0);
    }

    private void stubJson(MappingBuilder request, JsonNode response) {
        provider.stubFor(request.willReturn(okJson(mapper.writeValueAsString(response))));
    }

    private MappingBuilder request(Lookup operation) {
        return operation == Lookup.AUTOCOMPLETE ? autocomplete("Warszawa", "countrycode:pl") : details();
    }

    private void invoke(Lookup operation) {
        if (operation == Lookup.AUTOCOMPLETE) client.search("Warszawa", "PL");
        else client.getById(WARSAW_ID);
    }

    private MappingBuilder autocomplete(String query, String bias) {
        return get(urlPathEqualTo(AUTOCOMPLETE)).withQueryParam("text", equalTo(query))
                .withQueryParam("type", equalTo("city")).withQueryParam("limit", equalTo("10"))
                .withQueryParam("lang", equalTo("en")).withQueryParam("format", equalTo("json"))
                .withQueryParam("bias", equalTo(bias)).withQueryParam("apiKey", equalTo(API_KEY));
    }

    private MappingBuilder details() {
        return get(urlPathEqualTo(PLACE_DETAILS)).withQueryParam("id", equalTo(WARSAW_ID))
                .withQueryParam("features", equalTo("details")).withQueryParam("lang", equalTo("en"))
                .withQueryParam("apiKey", equalTo(API_KEY));
    }

    private ResponseDefinitionBuilder jsonFile(String name) {
        return aResponse().withHeader("Content-Type", "application/json; charset=UTF-8").withBodyFile(name);
    }

    private void assertSingleRequest(String... queryKeys) {
        assertThat(provider.getAllServeEvents()).singleElement()
                .satisfies(event -> assertThat(event.getRequest().getQueryParams()).containsOnlyKeys(queryKeys));
    }

    private JsonNode fixture(String name) throws IOException {
        try (var input = new ClassPathResource("wiremock/geoapify/__files/" + name).getInputStream()) {
            JsonNode response = mapper.readTree(input);
            assertThat(response).as("Geoapify fixture %s", name).isInstanceOf(ObjectNode.class);
            return response;
        }
    }
}
