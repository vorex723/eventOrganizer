package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.city.City;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.config.properties.SeedProperties;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import com.mazurek.eventOrganizer.user.Role;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("local")
public class DataInitializer implements CommandLineRunner {
    private final CityRepository cityRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;
    private final Clock clock;

    @Override
    public void run(String... args) throws Exception {
        if (!seedProperties.isLocalDataEnabled()) {
            log.info("Local seed data is disabled, skipping initialization");
            return;
        }
        initializeCity();
        initializeRoles();
        initializeNormalUser();
        initializeAdminUser();
    }
    private void initializeCity(){
        if (cityRepository.findByIgnoreCaseName("Rzeszow").isEmpty()) {
            cityRepository.save(new City("Rzeszow"));
        }
    }

    private void initializeRoles(){
        if (roleRepository.count() == 0) {
            log.info("Initializing roles...");

            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);

            Role adminRole = new Role();
            adminRole.setName("ROLE_ADMIN");
            roleRepository.save(adminRole);

            Role moderatorRole = new Role();
            moderatorRole.setName("ROLE_MODERATOR");
            roleRepository.save(moderatorRole);

            log.info("Roles initialized: ROLE_USER, ROLE_ADMIN, ROLE_MODERATOR");
        } else {
            log.info("Roles already exist ({}), skipping initialization", roleRepository.count());
        }
    }
    private void initializeNormalUser(){
        String normalEmail = "normal@eventorganizer.com";

        if (userRepository.findByEmail(normalEmail).isEmpty()) {
            log.info("Creating normal user...");

            City cityRzeszow = cityRepository.findByIgnoreCaseName("Rzeszow").orElseThrow(CityNotFoundException::new);
            Instant userCreateDate = clock.instant();

            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));

            User normal = User.builder()
                    .email(normalEmail)
                    .password(passwordEncoder.encode("Normal123@"))
                    .firstName("Normal")
                    .lastName("User")
                    .homeCity(cityRzeszow)
                    .createdAt(userCreateDate)
                    .lastCredentialsChangeTime(userCreateDate)
                    .timeZone("Europe/Warsaw")
                    .banned(false)
                    .activated(true)
                    .roles(Set.of(userRole))
                    .build();

            userRepository.save(normal);
            cityRepository.save(cityRzeszow);

            log.info("Local normal user created with email {}", normalEmail);
        } else {
            log.info("Normal user already exists, skipping initialization");
        }
    }

    private void initializeAdminUser(){
        String adminEmail = "admin@eventorganizer.com";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Creating admin user...");
            City cityRzeszow = cityRepository.findByIgnoreCaseName("Rzeszow").orElseThrow(CityNotFoundException::new);

            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("ROLE_USER not found"));
            Instant userCreateDate = clock.instant();
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin123@"))
                    .firstName("Admin")
                    .lastName("User")
                    .homeCity(cityRzeszow)
                    .createdAt(userCreateDate)
                    .lastCredentialsChangeTime(userCreateDate)
                    .timeZone("Europe/Warsaw")
                    .banned(false)
                    .activated(true)
                    .roles(Set.of(adminRole, userRole))
                    .build();

            userRepository.save(admin);
            cityRepository.save(cityRzeszow);

            log.info("Local admin user created with email {}", adminEmail);
        } else {
            log.info("Admin user already exists, skipping initialization");
        }
    }

}
