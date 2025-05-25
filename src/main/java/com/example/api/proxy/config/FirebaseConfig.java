package com.example.api.proxy.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY");
            System.out.println("FIREBASE_SERVICE_ACCOUNT_KEY is set: " + (serviceAccountJson != null));
            if (serviceAccountJson == null || serviceAccountJson.isEmpty()) {
                throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_KEY não configurado");
            }
            // Convert the JSON string to an InputStream
            ByteArrayInputStream serviceAccountStream = new ByteArrayInputStream(
                serviceAccountJson.getBytes(StandardCharsets.UTF_8)
            );
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp initialized successfully");
            } else {
                System.out.println("FirebaseApp already initialized");
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar Firebase: " + e.getMessage(), e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}
