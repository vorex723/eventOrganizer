package com.mazurek.eventOrganizer.city;

import com.mazurek.eventOrganizer.city.cityLookupClient.CityLookupClient;
import com.mazurek.eventOrganizer.city.cityLookupClient.CitySearchResult;
import com.mazurek.eventOrganizer.city.cityLookupClient.ResolvedCity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.time.ZoneId;

import static com.mazurek.eventOrganizer.testData.TestFailureHelper.requirePresent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ActiveProfiles("test")
@SpringBootTest
@Tag("external")
@DisplayName("Geoapify lookup client integration tests:")
public class GeoapifyLookupClientIntegrationTest {

    @Autowired
    @Qualifier("geoapifyCityLookupClient")
    private CityLookupClient cityLookupClient;

    @Test
    void whenSearchingPolishNameWithCountryBiasShouldFindWarsaw() {
        List<CitySearchResult> results = cityLookupClient.search("Warszawa", "PL");

        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(result -> "PL".equals(result.countryCode())
                && "Warsaw".equals(result.displayName()));
    }

    @Test
    void whenSelectingWarsawShouldResolveByExternalId() {
        CitySearchResult searchResult = requirePresent(cityLookupClient.search("Warszawa", "PL").stream()
                .filter(result -> "PL".equals(result.countryCode()) && "Warsaw".equals(result.displayName()))
                .findFirst(), "Expected Geoapify search to return Warsaw in Poland");
        ResolvedCity resolved = cityLookupClient.getById(searchResult.externalId());

        assertThat(resolved).isNotNull();
        assertThat(resolved.externalId()).isEqualTo(searchResult.externalId());
        assertThat(resolved.countryCode()).isEqualTo("PL");
        assertThat(resolved.name()).isEqualTo("Warsaw");
        assertThat(resolved.timeZoneId()).isEqualTo("Europe/Warsaw");
        assertThatCode(() -> ZoneId.of(resolved.timeZoneId())).doesNotThrowAnyException();
    }
}
