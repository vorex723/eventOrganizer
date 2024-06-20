package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

//@Configuration
//@EnableConfigurationProperties(FirebaseProperties.class)
@Component
@RequiredArgsConstructor
public class FcmInitializer {
    @Value("${gcp.firebase.service-account}")
    private String firebaseConfigPath;

    private final ResourceLoader resourceLoader;

    private Resource serviceAccount;

    @PostConstruct
    public void initialize(){
        try{
            serviceAccount = resourceLoader.getResource(firebaseConfigPath);

            FirebaseOptions firebaseOptions2 = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount.getInputStream()))
                    .build();
            if (FirebaseApp.getApps().isEmpty()){
                FirebaseApp.initializeApp(firebaseOptions2);
                System.out.println("Firebase app initialized.");
            }
        } catch (IOException exception){
            System.out.println(exception);
            System.out.println("Problem with key file.");
        }

    }

}
