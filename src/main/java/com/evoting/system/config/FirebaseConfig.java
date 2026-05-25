package com.evoting.system.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount =
                getClass().getResourceAsStream(
                    "/firebase-service-account.json");
            
            if (serviceAccount == null) {
                System.err.println(
                    "Firebase service account file not found!");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials
                    .fromStream(serviceAccount))
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println(
                    "Firebase initialized successfully!");
            }
        } catch (Exception e) {
            System.err.println(
                "Firebase initialization failed: " + e.getMessage());
        }
    }
}
