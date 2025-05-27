// src/main/java/com/example/api/proxy/push/PushTestMain.java
package com.example.api.proxy.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;

import java.io.FileInputStream;
import java.io.IOException;

public class PushTestMain {

    public static void main(String[] args) throws Exception {
        String testToken = System.getenv("FCM_TOKEN");
        if (testToken == null || testToken.isEmpty()) {
            if (args.length > 0) {
                testToken = args[0];
            } else {
                System.err.println("❌ FCM_TOKEN environment variable or command-line argument required");
                System.exit(1);
            }
        }
        System.out.println("Using FCM_TOKEN: " + testToken);

        try {
            String serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
            if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
                serviceAccountPath = "C:\\JAVA\\CHAVES-SEGURAS\\serviceAccountKey.json";
            }
            System.out.println("Using FIREBASE_SERVICE_ACCOUNT_PATH: " + serviceAccountPath);
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

        FirebaseMessaging messaging = FirebaseMessaging.getInstance();

        WebpushNotification webNotification = WebpushNotification.builder()
                .setTitle("🚀 Teste WebPush")
                .setBody("Notificação enviada com sucesso para o navegador!")
                .setRequireInteraction(true)
                .build();

        WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(webNotification)
                .setFcmOptions(WebpushFcmOptions.builder()
                        .setLink("http://localhost:8080")
                        .build())
                .build();

        Message message = Message.builder()
                .setToken(testToken)
                .setWebpushConfig(webpushConfig)
                .putData("url", "http://localhost:8080")
                .putData("tag", "test-notification")
                .build();

        try {
            String response = messaging.send(message);
            System.out.println("✅ Notificação WebPush enviada com sucesso: " + response);
        } catch (FirebaseMessagingException e) {
            System.err.println("❌ Erro ao enviar notificação: " + e.getMessage());
            System.err.println("FCM Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
    }
}