package com.example.api.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Objeto para registrar token de notificação push")
public class PushTokenRequest {
    @Schema(description = "Código de rastreamento", example = "AB123456789BR")
    @Pattern(regexp = "[A-Z]{2}\\d{9}[A-Z]{2}", message = "Código de rastreamento deve seguir o formato XX999999999XX")
    private String trackingCode;

    @Schema(description = "Token de notificação push", example = "fcm-token-example")
    @NotNull(message = "Token de notificação push é obrigatório")
    private String pushToken;

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }
}
