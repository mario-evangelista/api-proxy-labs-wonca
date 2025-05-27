package com.example.api.proxy.service;

import com.example.api.proxy.entity.TrackingData;
import com.example.api.proxy.exception.InvalidTrackingCodeException;
import com.example.api.proxy.exception.PushNotificationException;
import com.example.api.proxy.exception.TrackingNotFoundException;
import com.example.api.proxy.repository.TrackingDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingService.class);
    private static final String TRACKING_CODE_PATTERN = "[A-Z]{2}\\d{9}[A-Z]{2}"; // Ex.: AB123456789BR
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"); // Moved to class level

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private FirebaseMessaging firebaseMessaging;

    @Autowired
    private TrackingDataRepository trackingDataRepository;

    @Value("${correios.api.url}")
    private String correiosApiUrl;

    @Value("${correios.api.key}")
    private String correiosApiToken;

    public String trackPackage(String trackingCode) {
        // Validação do trackingCode
        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            throw new InvalidTrackingCodeException("Código de rastreamento é obrigatório");
        }
        if (!trackingCode.matches(TRACKING_CODE_PATTERN)) {
            throw new InvalidTrackingCodeException("Código de rastreamento inválido: deve seguir o formato XX999999999XX");
        }

        String apiResponse;
        try {
            apiResponse = fetchTrackingFromCorreios(trackingCode);
        } catch (HttpClientErrorException e) {
            logger.error("Erro HTTP na API dos Correios para código {}: {}", trackingCode, e.getStatusCode());
            throw new TrackingNotFoundException("Erro ao consultar rastreamento: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Erro inesperado ao consultar rastreamento para código {}: {}", trackingCode, e.getMessage(), e);
            throw new TrackingNotFoundException("Erro interno ao consultar rastreamento");
        }

        String extractedStatus = extractStatus(apiResponse);

        Optional<TrackingData> optional = trackingDataRepository.findByTrackingCode(trackingCode);
        TrackingData data = optional.orElse(new TrackingData());
        data.setTrackingCode(trackingCode);
        data.setLastStatus(extractedStatus);
        data.setLastUpdated(LocalDateTime.now()); // Optional: Store timestamp
        trackingDataRepository.save(data);

        return apiResponse;
    }

    /*public void registerPushToken(String trackingCode, String pushToken) {
        // Validação dos parâmetros
        if (trackingCode == null || trackingCode.trim().isEmpty()) {
            throw new InvalidTrackingCodeException("Código de rastreamento é obrigatório");
        }
        if (!trackingCode.matches(TRACKING_CODE_PATTERN)) {
            throw new InvalidTrackingCodeException("Código de rastreamento inválido: deve seguir o formato XX999999999XX");
        }
        if (pushToken == null || pushToken.trim().isEmpty()) {
            throw new InvalidTrackingCodeException("Token de notificação push é obrigatório");
        }

        Optional<TrackingData> optional = trackingDataRepository.findByTrackingCode(trackingCode);
        TrackingData data = optional.orElse(new TrackingData());
        data.setTrackingCode(trackingCode);
        data.setPushToken(pushToken);
        trackingDataRepository.save(data);
    }*/

    public void registerPushToken(String trackingCode, String pushToken) {
        logger.info("Registering push token: {} for tracking code: {}", pushToken, trackingCode);
        if (trackingCode == null) {
            logger.warn("Skipping token registration: trackingCode is null");
            return; // Skip persistence if no tracking code
        }
        TrackingData data = trackingDataRepository.findByTrackingCode(trackingCode)
                .orElse(new TrackingData());
        data.setTrackingCode(trackingCode);
        data.setPushToken(pushToken);
        trackingDataRepository.save(data);
    }

    /*
    //@Scheduled(fixedRate = 180000)
    @Scheduled(fixedRate = 600000)// 600000 10 minutos
    public void checkForUpdates() {
        List<TrackingData> allData = trackingDataRepository.findAll();
        for (TrackingData data : allData) {
            try {
                String trackingCode = data.getTrackingCode();
                if (!trackingCode.matches(TRACKING_CODE_PATTERN)) {
                    logger.warn("Código de rastreamento inválido no banco: {}", trackingCode);
                    continue;
                }

                String apiResponse = fetchTrackingFromCorreios(trackingCode);
                String currentStatus = extractStatus(apiResponse);

                if (!currentStatus.equals(data.getLastStatus())) {
                    String now = LocalDateTime.now().format(formatter);
                    String message = String.format("Novo status: %s\nCódigo: %s\nAtualizado em: %s",
                            currentStatus, trackingCode, now);
                    String redirectUrl = "http://localhost:3000/track?code=" + trackingCode;

                    if (data.getPushToken() != null && !data.getPushToken().isEmpty()) {
                        sendPushNotification(data.getPushToken(), "Atualização no rastreio", message, redirectUrl);
                    } else {
                        logger.warn("Nenhum token de notificação registrado para o código: {}", trackingCode);
                    }

                    data.setLastStatus(currentStatus);
                    data.setLastUpdated(LocalDateTime.now()); // Optional: Update timestamp
                    trackingDataRepository.save(data);
                }
            } catch (TrackingNotFoundException e) {
                logger.error("Erro ao verificar atualizações para {}: {}", data.getTrackingCode(), e.getMessage());
            } catch (Exception e) {
                logger.error("Erro inesperado ao verificar atualizações para {}: {}", data.getTrackingCode(), e.getMessage(), e);
            }
        }
    }*/

    //@Scheduled(fixedRate = 600000)
    /*@Scheduled(fixedRate = 180000)
    public void checkForUpdates() {
        List<TrackingData> allData = trackingDataRepository.findAll();
        for (TrackingData data : allData) {
            try {
                String trackingCode = data.getTrackingCode();
                if (!trackingCode.matches(TRACKING_CODE_PATTERN)) {
                    logger.warn("Código de rastreamento inválido no banco: {}", trackingCode);
                    continue;
                }

                String apiResponse = fetchTrackingFromCorreios(trackingCode);
                String currentStatus = extractStatus(apiResponse);

                if (!currentStatus.equals(data.getLastStatus())) {
                    String now = LocalDateTime.now().format(formatter);
                    String message = String.format("Novo status: %s\nCódigo: %s\nAtualizado em: %s",
                            currentStatus, trackingCode, now);
                    String redirectUrl = "http://localhost:3000/track?code=" + trackingCode;

                    if (data.getPushToken() != null && !data.getPushToken().isEmpty()) {
                        try {
                            sendPushNotification(data.getPushToken(), "Atualização no rastreio", message, redirectUrl);
                        } catch (PushNotificationException e) {
                            logger.warn("Falha ao enviar notificação para {}: {}", trackingCode, e.getMessage());
                        }
                    } else {
                        logger.warn("Nenhum token de notificação registrado para o código: {}", trackingCode);
                    }

                    data.setLastStatus(currentStatus);
                    data.setLastUpdated(LocalDateTime.now());
                    trackingDataRepository.save(data);
                }
            } catch (TrackingNotFoundException e) {
                logger.error("Erro ao verificar atualizações para {}: {}", data.getTrackingCode(), e.getMessage());
            } catch (Exception e) {
                logger.error("Erro inesperado ao verificar atualizações para {}: {}", data.getTrackingCode(), e.getMessage(), e);
            }
        }
    }*/

    @Scheduled(fixedRate = 180000)
    //@Scheduled(fixedRate = 600000)
    public void checkForUpdates() {
        List<TrackingData> trackingDataList = trackingDataRepository.findAll();
        // Definir o formatador para data/hora no padrão brasileiro
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                .withZone(ZoneId.of("America/Sao_Paulo"));

        for (TrackingData data : trackingDataList) {
            try {
                String trackingCode = data.getTrackingCode();
                if (trackingCode == null) continue; // Skip entries without tracking code
                String currentStatus = trackPackage(trackingCode);
                String newStatus = extractStatus(currentStatus);
                if (!newStatus.equals(data.getLastStatus()) && data.getPushToken() != null) {
                    String title = "Atualização no rastreio";
                    // Incluir novo status, código de rastreamento e data/hora atual
                    String currentDateTime = LocalDateTime.now().format(formatter);
                    String body = "Novo status: " + newStatus + "\nCódigo: " + trackingCode + "\nData/Hora: " + currentDateTime;
                    String url = "http://localhost:3000/track?code=" + trackingCode;
                    sendPushNotification(data.getPushToken(), title, body, trackingCode, url);
                    data.setLastStatus(newStatus);
                    data.setLastUpdated(LocalDateTime.now());
                    trackingDataRepository.save(data);
                }
            } catch (Exception e) {
                logger.warn("Falha ao verificar atualizações para {}: {}", data.getTrackingCode(), e.getMessage());
            }
        }
    }

    /*public void checkForUpdates() {
        List<TrackingData> trackingDataList = trackingDataRepository.findAll();
        for (TrackingData data : trackingDataList) {
            try {
                String trackingCode = data.getTrackingCode();
                if (trackingCode == null) continue; // Skip entries without tracking code
                String currentStatus = trackPackage(trackingCode);
                String newStatus = extractStatus(currentStatus);
                if (!newStatus.equals(data.getLastStatus()) && data.getPushToken() != null) {
                    String title = "Atualização no rastreio";
                    String body = "Novo status: " + newStatus;
                    String url = "http://localhost:3000/track?code=" + trackingCode;
                    sendPushNotification(data.getPushToken(), title, body, trackingCode, url);
                    data.setLastStatus(newStatus);
                    data.setLastUpdated(LocalDateTime.now());
                    trackingDataRepository.save(data);
                }
            } catch (Exception e) {
                logger.warn("Falha ao verificar atualizações para {}: {}", data.getTrackingCode(), e.getMessage());
            }
        }
    }*/

    private String fetchTrackingFromCorreios(String trackingCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Apikey " + correiosApiToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("code", trackingCode);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(correiosApiUrl, entity, String.class);
            logger.info("Resposta API para {}: {}", trackingCode, response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            logger.error("Erro HTTP na API dos Correios para {}: {} - {}", trackingCode, e.getStatusCode(), e.getResponseBodyAsString());
            throw new TrackingNotFoundException("Erro ao consultar API dos Correios: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Erro inesperado ao consultar rastreamento para {}: {}", trackingCode, e.getMessage(), e);
            throw new TrackingNotFoundException("Erro interno ao consultar rastreamento");
        }
    }

    private String extractStatus(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonResponse);

            JsonNode innerJsonNode = root.path("json");
            if (innerJsonNode.isMissingNode() || innerJsonNode.isNull()) {
                logger.warn("Chave 'json' não encontrada na resposta: {}", jsonResponse);
                throw new TrackingNotFoundException("Formato de resposta inválido: chave 'json' não encontrada");
            }

            JsonNode innerRoot = mapper.readTree(innerJsonNode.asText());

            JsonNode eventos = innerRoot.path("eventos");
            if (eventos.isArray() && eventos.size() > 0) {
                String status = eventos.get(0).path("descricao").asText("Status não encontrado");
                logger.info("Status extraído de 'eventos': {}", status);
                return status;
            }

            JsonNode objetos = innerRoot.path("objetos");
            if (objetos.isArray() && objetos.size() > 0) {
                eventos = objetos.get(0).path("eventos");
                if (eventos.isArray() && eventos.size() > 0) {
                    String status = eventos.get(0).path("descricao").asText("Status não encontrado");
                    logger.info("Status extraído de 'objetos -> eventos': {}", status);
                    return status;
                }
            }

            logger.warn("Nenhum status encontrado no JSON interno: {}", innerJsonNode.asText());
            throw new TrackingNotFoundException("Nenhum status encontrado na resposta da API");
        } catch (Exception e) {
            logger.error("Erro ao extrair status do JSON: {} | JSON: {}", e.getMessage(), jsonResponse);
            throw new TrackingNotFoundException("Erro ao processar resposta da API");
        }
    }

    /*public void sendPushNotification(String token, String title, String body, String redirectUrl) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Token de notificação inválido ou ausente");
            throw new PushNotificationException("Token de notificação inválido");
        }

        logger.info("Enviando notificação para token: {} | Título: {} | Corpo: {} | URL: {}", token, title, body, redirectUrl);

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("url", redirectUrl);

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .build();

        try {
            String response = firebaseMessaging.send(message);
            logger.info("Notificação enviada com sucesso: {}", response);
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação push para token {}: {}", token, e.getMessage(), e);
            throw new PushNotificationException("Falha ao enviar notificação push: " + e.getMessage());
        }
    }*/

    /*public void sendPushNotification(String token, String title, String body, String redirectUrl) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Token de notificação inválido ou ausente");
            throw new PushNotificationException("Token de notificação inválido");
        }

        logger.info("Enviando notificação para token: {} | Título: {} | Corpo: {} | URL: {}", token, title, body, redirectUrl);

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("url", redirectUrl);

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putAllData(data)
                .setWebpushConfig(WebpushConfig.builder()
                        .setFcmOptions(WebpushFcmOptions.builder()
                                .setLink(redirectUrl)
                                .build())
                        .build())
                .build();

        try {
            String response = firebaseMessaging.send(message);
            logger.info("Notificação enviada com sucesso: {}", response);
        } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
            logger.error("Erro ao enviar notificação push para token {}: {}", token, e.getMessage(), e);
            // Handle invalid or unregistered tokens
            if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT||
                    e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                logger.warn("Token inválido detectado: {}. Removendo do banco.", token);
                trackingDataRepository.findByPushToken(token).ifPresent(trackingData -> { // Renamed to trackingData
                    trackingData.setPushToken(null);
                    trackingDataRepository.save(trackingData);
                });
            }
            throw new PushNotificationException("Falha ao enviar notificação push: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erro inesperado ao enviar notificação push para token {}: {}", token, e.getMessage(), e);
            throw new PushNotificationException("Falha ao enviar notificação push: " + e.getMessage());
        }
    }*/

    public void sendPushNotification(String token, String title, String body, String trackingCode, String url) throws FirebaseMessagingException {
        try {
            logger.info("Tentando enviar notificação para token: {}", token);
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("url", url)
                    .putData("trackingCode", trackingCode) // Adicionar explicitamente o trackingCode no payload
                    .setToken(token)
                    .build();
            String response = firebaseMessaging.send(message);
            logger.info("Notificação enviada com sucesso para token: {} | Resposta: {}", token, response);
        } catch (FirebaseMessagingException e) {
            logger.error("Erro ao enviar notificação push para token {}: {}", token, e.getMessage());
            if (e.getMessagingErrorCode() == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED ||
                    e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                logger.warn("Token inválido detectado: {}. Removendo do banco.", token);
                trackingDataRepository.findByPushToken(token).ifPresent(trackingData -> {
                    trackingData.setPushToken(null);
                    trackingDataRepository.save(trackingData);
                });
            }
            throw e;
        }
    }

    /*@PostConstruct
    public void clearInvalidTokens() {
        trackingDataRepository.findAll().forEach(data -> {
            if (data.getPushToken() != null && data.getPushToken().equals("eKnnhAlFejHRc-VtdqioBB:APA91bFzzCM0IPgTt43nO8ND5eF7_wcpd2tR3jJoHtZWp45TZ2f9-dHBq5yVuYNZk-Uip4GhfIDpngoBP7J8jZNJu7aagX0OhNFl0_tkkKLLcloo-myWYgk")) {
                data.setPushToken(null);
                trackingDataRepository.save(data);
                logger.info("Cleared invalid token for tracking code: {}", data.getTrackingCode());
            }
        });
    }*/

    @PostConstruct
    public void clearInvalidTokens() {
        trackingDataRepository.findAll().forEach(data -> {
            if (data.getPushToken() != null && (
                    data.getPushToken().equals("euOcNlF4poJ2nEh-RgYOwv:APA91bG0S8Sw6IUTzzrCfx1HP_fT2mQeXiZyLcY-lVbyMULWeEtPHSlCcvIA8zcfRzUgFLZWU3kQ2jXsbfE0N-W0YZ6db5lYRPqyPOIBATBWE4JwZW8KiVg") ||
                            data.getPushToken().equals("eKnnhAlFejHRc-VtdqioBB:APA91bFzzCM0IPgTt43nO8ND5eF7_wcpd2tR3jJoHtZWp45TZ2f9-dHBq5yVuYNZk-Uip4GhfIDpngoBP7J8jZNJu7aagX0OhNFl0_tkkKLLcloo-myWYgk") ||
                            data.getPushToken().equals("e_LHBfVsDTGcxF7kfTfxvD:APA91bFeRp-kQaWXCtsJkQltkqf2duAgDbuOsfsdrXxurrhf9V04GX01vtoZS0j-qhyunrzlWUxvCOlHNmFAgrgnemLqp00pkRkD6MwJmqzRvJPwhvw8pGY"))) {
                data.setPushToken(null);
                trackingDataRepository.save(data);
                logger.info("Cleared invalid token for tracking code: {}", data.getTrackingCode());
            }
        });
    }

}