package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import com.mazurek.eventOrganizer.config.seed.LocalDemoSeeder;
import com.mazurek.eventOrganizer.config.seed.LocalSeedReportPrinter;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local")
public class DataInitializer implements CommandLineRunner {
    private final CityService cityService;
    private final RoleRepository roleRepository;
    private final SeedProperties seedProperties;
    private final LocalDemoSeeder demoSeeder;
    private final LocalSeedReportPrinter reportPrinter;

    @Override
    public void run(String... args) {
        // Registration needs every role, also with disabled demos or a partially populated DB.
        for (String name : new String[]{"ROLE_USER", "ROLE_ADMIN", "ROLE_MODERATOR"}) {
            if (roleRepository.findByName(name).isEmpty()) {
                roleRepository.save(new Role(name));
            }
        }
        if (!seedProperties.isLocalDataEnabled()) {
            log.info("Local seed data is disabled, skipping initialization");
            return;
        }
        if (seedProperties.getCityExternalId() == null || seedProperties.getCityExternalId().isBlank()) {
            throw new IllegalArgumentException("Set APP_SEED_CITY_EXTERNAL_ID or disable app.seed.local-data-enabled.");
        }
        // Validate before writes; external city lookup does not hold the demo transaction open.
        reportPrinter.validateBaseUrl();
        var city = cityService.resolve(seedProperties.getCityExternalId());
        var report = demoSeeder.seed(city);
        // The separate transactional bean has committed before any resource links are logged.
        reportPrinter.print(report);
    }
}
