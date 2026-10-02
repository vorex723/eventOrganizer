package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerUnitTest {
    private final CityService cities = mock(CityService.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeedProperties properties = new SeedProperties();
    private final DataInitializer initializer = new DataInitializer(cities, roles, users,
            mock(PasswordEncoder.class), properties, Clock.systemUTC());

    @Test
    void disabledSampleDataStillCreatesRolesWithoutLookingUpCities() throws Exception {
        initializer.run();
        verify(roles, times(3)).save(any(Role.class));
        verifyNoInteractions(cities, users);
    }

    @Test
    void seedRequiresRealExternalIdentifierInsteadOfFallingBackToName() {
        properties.setLocalDataEnabled(true);
        assertThatIllegalArgumentException().isThrownBy(initializer::run)
                .withMessageContaining("APP_SEED_CITY_EXTERNAL_ID");
        verifyNoInteractions(cities, users);
    }

    @Test
    void configuredSeedResolvesThroughCityServiceAndDoesNotDuplicateExistingUsers() throws Exception {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        when(users.findByEmail(anyString())).thenReturn(java.util.Optional.of(mock(com.mazurek.eventOrganizer.user.User.class)));
        initializer.run();
        verify(cities).resolve("selected-provider-place-id");
        verify(users, never()).save(any());
    }

    @Test
    void bothSampleAccountsUseTheirSeedCityTimezone() throws Exception {
        properties.setLocalDataEnabled(true);
        properties.setCityExternalId("selected-provider-place-id");
        var city = com.mazurek.eventOrganizer.testData.builders.CityTestBuilder.warsaw()
                .timeZoneId("America/New_York").build();
        when(cities.resolve("selected-provider-place-id")).thenReturn(city);
        when(roles.findByName(anyString())).thenAnswer(invocation -> java.util.Optional.of(new Role(invocation.getArgument(0))));
        initializer.run();
        var accounts = org.mockito.ArgumentCaptor.forClass(com.mazurek.eventOrganizer.user.User.class);
        verify(users, times(2)).save(accounts.capture());
        assertThat(accounts.getAllValues()).allSatisfy(user -> {
            assertThat(user.getHomeCity()).isSameAs(city);
            assertThat(user.getTimeZone()).isEqualTo("America/New_York");
        });
    }
}
