package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import com.mazurek.eventOrganizer.config.seed.LocalDemoSeeder;
import com.mazurek.eventOrganizer.config.seed.LocalSeedReportPrinter;
import com.mazurek.eventOrganizer.testData.builders.CityTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.LocalSeedReportTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.SeedPropertiesTestBuilder;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DataInitializerUnitTest contracts:")
class DataInitializerUnitTest {
    private final CityService cities = mock(CityService.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final LocalDemoSeeder seeder = mock(LocalDemoSeeder.class);
    private final LocalSeedReportPrinter printer = mock(LocalSeedReportPrinter.class);
    private final SeedProperties properties = new SeedPropertiesTestBuilder()
            .cityExternalId(null)
            .apiBaseUrl(null)
            .build();
    private final DataInitializer initializer = new DataInitializer(cities, roles, properties, seeder, printer);

    @Test
    void whenDemoIsDisabledShouldCreateRolesWithoutCityLookup() {
        initializer.run();
        var captured = org.mockito.ArgumentCaptor.forClass(Role.class);
        verify(roles, times(3)).save(captured.capture());
        assertThat(captured.getAllValues()).extracting(Role::getName)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR");
        verifyNoInteractions(cities, seeder, printer);
    }

    @Test
    void whenRolesAlreadyExistShouldCreateOnlyMissingRole() {
        when(roles.findByName("ROLE_USER")).thenReturn(Optional.of(new RoleTestBuilder()
                .id(null)
                .name("ROLE_USER")
                .build()));
        when(roles.findByName("ROLE_ADMIN")).thenReturn(Optional.of(new RoleTestBuilder()
                .id(null)
                .name("ROLE_ADMIN")
                .build()));
        initializer.run();
        var captured = org.mockito.ArgumentCaptor.forClass(Role.class);
        verify(roles).save(captured.capture());
        assertThat(captured.getValue().getName()).isEqualTo("ROLE_MODERATOR");
        verifyNoInteractions(cities, seeder, printer);
    }

    @Test
    void whenDemoIsEnabledWithoutCityIdentifierShouldRejectInitialization() {
        properties.setLocalDataEnabled(true);
        assertThatIllegalArgumentException().isThrownBy(initializer::run).withMessageContaining("APP_SEED_CITY_EXTERNAL_ID");
        verifyNoInteractions(cities, seeder, printer);
    }

    @Test
    void whenSeedingCompletesShouldPrintResolvedResourceReport() {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        var city = CityTestBuilder.warsaw().build();
        var report = new LocalSeedReportTestBuilder()
                .accounts(List.of())
                .resources(List.of())
                .build();
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
    void whenSeedingFailsShouldNotPrintPhantomResources() {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        when(cities.resolve(properties.getCityExternalId())).thenReturn(CityTestBuilder.warsaw().build());
        when(seeder.seed(any())).thenThrow(new IllegalStateException("seed rollback"));
        assertThatIllegalStateException().isThrownBy(initializer::run).withMessage("seed rollback");
        verify(printer, never()).print(any());
    }

    @Test
    void whenReportUrlIsInvalidShouldRejectBeforeCityLookupOrWrites() {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        doThrow(new IllegalArgumentException("invalid base URL")).when(printer).validateBaseUrl();
        assertThatIllegalArgumentException().isThrownBy(initializer::run);
        verifyNoInteractions(cities, seeder);
    }
}
