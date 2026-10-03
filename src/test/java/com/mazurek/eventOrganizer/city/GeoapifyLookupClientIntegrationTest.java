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

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Tag("external")
@DisplayName("Geoapify lookup client integration tests:")
public class GeoapifyLookupClientIntegrationTest {

    @Autowired
    @Qualifier("geoapifyCityLookupClient")
    private CityLookupClient cityLookupClient;

    @Test
    void shouldFindWarsawUsingPolishNameAndCountryBias() {
        List<CitySearchResult> results = cityLookupClient.search("Warszawa", "PL");

        assertFalse(results.isEmpty());

        assertTrue(results.stream().anyMatch(result -> "PL".equals(result.countryCode())));
    }

    @Test
    void shouldResolveSelectedWarsawByExternalId() {
        CitySearchResult searchResult = cityLookupClient.search("Warszawa", "PL").stream()
                .filter(result -> "PL".equals(result.countryCode()) && "Warsaw".equals(result.displayName()))
                .findFirst()
                .orElseThrow();
        ResolvedCity resolved = cityLookupClient.getById(searchResult.externalId());

        assertEquals(searchResult.externalId(), resolved.externalId());
        assertEquals("PL", resolved.countryCode());
        assertEquals("Warsaw", resolved.name());
        assertNotNull(resolved.timeZoneId());
        assertDoesNotThrow(() -> ZoneId.of(resolved.timeZoneId()));
    }
}
