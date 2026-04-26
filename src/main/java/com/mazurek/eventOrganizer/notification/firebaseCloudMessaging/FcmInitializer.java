package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.mazurek.eventOrganizer.config.properties.FirebaseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Profile("production")
@Slf4j
public class FcmInitializer {

    private final ResourceLoader resourceLoader;
    private final FirebaseProperties firebaseProperties;

    @PostConstruct
    public void initialize() {
        if (!firebaseProperties.isEnabled()) {
            log.info("Firebase integration is disabled");
            return;
        }

        try {
            Resource serviceAccount = resourceLoader.getResource(firebaseProperties.getServiceAccountLocation());
            if (!serviceAccount.exists()) {
                throw new IllegalStateException("Firebase service account resource does not exist: "
                        + firebaseProperties.getServiceAccountLocation());
            }
            FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(firebaseOptions);
                log.info("Firebase app initialized");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to initialize Firebase", exception);
        }
    }
}
