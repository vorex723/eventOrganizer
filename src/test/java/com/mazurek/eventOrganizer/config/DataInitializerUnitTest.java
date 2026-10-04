package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import com.mazurek.eventOrganizer.config.seed.LocalDemoSeeder;
import com.mazurek.eventOrganizer.config.seed.LocalSeedReport;
import com.mazurek.eventOrganizer.config.seed.LocalSeedReportPrinter;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerUnitTest {
    private final CityService cities = mock(CityService.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final LocalDemoSeeder seeder = mock(LocalDemoSeeder.class);
    private final LocalSeedReportPrinter printer = mock(LocalSeedReportPrinter.class);
    private final SeedProperties properties = new SeedProperties();
    private final DataInitializer initializer = new DataInitializer(cities, roles, properties, seeder, printer);

    @Test
    void disabledDemoStillCreatesRolesWithoutLookingUpCities() {
        initializer.run();
        verify(roles, times(3)).save(any(Role.class));
        verifyNoInteractions(cities, seeder, printer);
    }

    @Test
    void fillsOnlyMissingRoleInPartiallyPopulatedDatabase() {
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(new Role("ROLE_USER")));
        when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new Role("ROLE_ADMIN")));
        initializer.run();
        var captured = org.mockito.ArgumentCaptor.forClass(Role.class);
        verify(roles).save(captured.capture());
        assertThat(captured.getValue().getName()).isEqualTo("ROLE_MODERATOR");
        verifyNoInteractions(cities, seeder, printer);
    }

    @Test
    void enabledDemoRequiresExternalIdentifier() {
        properties.setLocalDataEnabled(true);
        assertThatIllegalArgumentException().isThrownBy(initializer::run).withMessageContaining("APP_SEED_CITY_EXTERNAL_ID");
        verifyNoInteractions(cities, seeder, printer);
    }

    @Test
    void printsOnlyAfterSeedCompletesAndResolvesCityOutsideSeeder() {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        var city = CityTestBuilder.warsaw().build();
        var report = new LocalSeedReport(List.of(), List.of());
        when(cities.resolve(properties.getCityExternalId())).thenReturn(city);
        when(seeder.seed(city)).thenReturn(report);
        initializer.run();
        var order = inOrder(printer, cities, seeder);
        order.verify(printer).validateBaseUrl();
        order.verify(cities).resolve(properties.getCityExternalId());
        order.verify(seeder).seed(city);
        order.verify(printer).print(report);
    }

    @Test
    void failingSeedDoesNotPrintPhantomResourceLinks() {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        when(seeder.seed(any())).thenThrow(new IllegalStateException("seed rollback"));
        assertThatIllegalStateException().isThrownBy(initializer::run).withMessage("seed rollback");
        verify(printer, never()).print(any());
    }

    @Test
    void invalidReportUrlFailsBeforeCityLookupOrDemoWrites() {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        doThrow(new IllegalArgumentException("invalid base URL")).when(printer).validateBaseUrl();
        assertThatIllegalArgumentException().isThrownBy(initializer::run);
        verifyNoInteractions(cities, seeder);
    }
}
