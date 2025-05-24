package com.example.api.proxy.service;

import com.example.api.proxy.entity.TrackingData;
import com.example.api.proxy.exception.InvalidTrackingCodeException;
import com.example.api.proxy.exception.PushNotificationException;
import com.example.api.proxy.exception.TrackingNotFoundException;
import com.example.api.proxy.repository.TrackingDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
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

    public void registerPushToken(String trackingCode, String pushToken) {
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
    }


    //@Scheduled(fixedRate = 180000)
    //@Scheduled(fixedRate = 600000)// 600000 10 minutos
    public void checkForUpdates() {
        List<TrackingData> allData = trackingDataRepository.findAll();
        for (TrackingData data : allData) {
            try {
                String trackingCode = data.getTrackingCode();
                if (!trackingCode.matches(TRACKING_CODE_PATTERN)) {
                    logger.warn("Código de rastreamento inválido no banco: {}", trackingCode);
                    continue; // Pula para o próximo registro
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
    }

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

    public void sendPushNotification(String token, String title, String body, String redirectUrl) {
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
    }
}