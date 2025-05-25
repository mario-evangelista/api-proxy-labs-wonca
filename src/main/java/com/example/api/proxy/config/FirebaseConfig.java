package com.example.api.proxy.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            String serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
            System.out.println("FIREBASE_SERVICE_ACCOUNT_PATH: " + serviceAccountPath);
            if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
                throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_PATH não configurado");
            }
            FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
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