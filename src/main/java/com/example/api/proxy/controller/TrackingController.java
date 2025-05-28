package com.example.api.proxy.controller;

import com.example.api.proxy.dto.ErrorResponse;
import com.example.api.proxy.dto.PushTokenRequest;
import com.example.api.proxy.dto.TrackRequest;
import com.example.api.proxy.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "http://localhost:3000")
//@CrossOrigin(origins = "https://rastreio-encomendas-correios.onrender.com")
@CrossOrigin(origins = "https://rastreio-encomendas-correios.onrender.com", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
@Tag(name = "Rastreamento", description = "Endpoints para rastreamento de pacotes e registro de tokens de notificação")
public class TrackingController {

    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);

    @Autowired
    private TrackingService trackingService;

    @Operation(summary = "Rastrear um pacote", description = "Realiza o rastreamento de um pacote com base no código fornecido.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rastreamento realizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Código de rastreamento inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Código de rastreamento não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/track")
    public ResponseEntity<?> trackPackage(@RequestBody TrackRequest request) {
        try {
            String result = trackingService.trackPackage(request.getCode());
            return ResponseEntity.ok().body(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Registrar token de notificação push", description = "Registra um token de notificação push associado a um código de rastreamento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token registrado com sucesso", content = @Content),
            @ApiResponse(responseCode = "400", description = "Parâmetros 'trackingCode' ou 'pushToken' ausentes", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register-push-token")
    public ResponseEntity<?> registerPushToken(@RequestBody PushTokenRequest request) {
        try {
            trackingService.registerPushToken(request.getTrackingCode(), request.getPushToken());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error registering token: " + e.getMessage());
        }
    }

    @PostMapping("/save-token")
    public ResponseEntity<?> saveToken(@RequestBody PushTokenRequest request) {
        try {
            trackingService.registerPushToken(request.getTrackingCode(), request.getPushToken());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving token: " + e.getMessage());
        }
    }

}
