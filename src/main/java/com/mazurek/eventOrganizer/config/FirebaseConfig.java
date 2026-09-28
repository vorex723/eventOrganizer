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
        Resource serviceAccountResource = resourceLoader
                .getResource(firebaseProperties.getServiceAccountLocation());

        if(!serviceAccountResource.exists())
            throw new IllegalStateException("Service account location does not exist: " + firebaseProperties.getServiceAccountLocation());

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
