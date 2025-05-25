package com.example.api.proxy.push;

import com.example.api.proxy.config.FirebaseConfig;
import com.google.firebase.messaging.*;

public class PushTestMain {

    public static void main(String[] args) throws Exception {
        FirebaseConfig config = new FirebaseConfig();
        FirebaseMessaging messaging = config.firebaseMessaging();

        String testToken = "eaxNSITPGy2AkXSSUhGbBl:APA91bHNIzW5rQuOISEAlL6oYIy44lroAU_-bFdciQYYtg2OiX9Y4RnmWoVzd-_3np2DVuGHCHc61TQsoVizzphcX4q52uux5Rj3dRoN91j7PF8ceuyvYFY";

        WebpushNotification webNotification = WebpushNotification.builder()
                .setTitle("🚀 Teste WebPush")
                .setBody("Notificação enviada com sucesso para o navegador!")
                .setIcon("https://firebase.google.com/images/social.png") // ícone opcional
                .setRequireInteraction(true) // NOTIFICAÇÃO FICA ATÉ AÇÃO DO USUÁRIO
                .build();

        WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(webNotification)
                .build();

        // Monta a mensagem com token
        Message message = Message.builder()
                .setToken(testToken)
                .setWebpushConfig(webpushConfig)
                .build();

        // Envia
        try {
            String response = messaging.send(message);
            System.out.println("✅ Notificação WebPush enviada com sucesso: " + response);
        } catch (FirebaseMessagingException e) {
            System.err.println("❌ Erro ao enviar notificação: " + e.getMessage());
            e.printStackTrace();
        }
    }
}