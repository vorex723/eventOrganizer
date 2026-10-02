package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupException;
import com.mazurek.eventOrganizer.city.cityLookupClient.GeoapifyCityLookupClient;
import com.mazurek.eventOrganizer.config.properties.GeoapifyProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoapifyCityLookupClientUnitTest {
    private MockRestServiceServer server;
    private GeoapifyCityLookupClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://geoapify.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeoapifyCityLookupClient(builder.build(),
                new GeoapifyProperties("test-key", "https://geoapify.example", 3000, 5000));
    }

    private void respondWithTimezone(String timezoneJson) {
        String response = """
                {"features":[{"properties":{
                  "feature_type":"details", "name":"Warsaw", "country_code":"pl",
                  "lat":52.2297, "lon":21.0122, "timezone":%s
                }}]}
                """.formatted(timezoneJson);
        server.expect(requestTo("https://geoapify.example/v2/place-details?id=place&features=details&lang=en&apiKey=test-key"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
    }

    @Test
    void readsNamedTimezoneFromDetailsAndIgnoresProviderOffsets() {
        respondWithTimezone("""
                {"name":"Europe/Warsaw", "offset_STD":"+01:00", "offset_DST":"+02:00"}
                """);
        var result = client.getById("place");
        assertThat(result.timeZoneId()).isEqualTo("Europe/Warsaw");
        assertThat(result.externalId()).isEqualTo("place");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\" \"}",
            "{\"name\":\"Invalid/Zone\"}", "{\"name\":\"UTC+02:00\"}", "{\"name\":\"+02:00\"}"})
    void missingInvalidOrOffsetTimezoneIsRejectedWithoutServerDefault(String timezoneJson) {
        respondWithTimezone(timezoneJson);
        assertThatThrownBy(() -> client.getById("place")).isInstanceOf(CityLookupException.class);
        server.verify();
    }
}
