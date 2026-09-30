package com.mazurek.eventOrganizer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
@Profile("production")
@ConditionalOnProperty(
        prefix = "app.firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;
    private final FirebaseProperties firebaseProperties;

    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp(){
        String serviceAccountLocation = firebaseProperties.getServiceAccountLocation();
        if (!StringUtils.hasText(serviceAccountLocation)
                || !serviceAccountLocation.startsWith("file:")) {
            throw new IllegalStateException(
                    "app.firebase.service-account-location must reference an external file: resource "
                            + "when Firebase is enabled in production."
            );
        }

        Resource serviceAccountResource = resourceLoader
                .getResource(serviceAccountLocation);

        if (!serviceAccountResource.exists() || !serviceAccountResource.isReadable()) {
            throw new IllegalStateException(
                    "Firebase service account file does not exist or is not readable: " + serviceAccountLocation
            );
        }

        try (InputStream inputStream = serviceAccountResource.getInputStream()){
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            return FirebaseApp.initializeApp(options);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize Firebase.");
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(){
        return FirebaseMessaging.getInstance(firebaseApp());
    }
}
