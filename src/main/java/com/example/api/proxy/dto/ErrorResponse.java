package com.example.api.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Objeto de resposta para erros na API")
public class ErrorResponse {
    @Schema(description = "Código de status HTTP", example = "400")
    private final int status;

    @Schema(description = "Mensagem de erro", example = "Código de rastreamento inválido")
    private final String message;

    @Schema(description = "Detalhes adicionais do erro", example = "O código fornecido não segue o formato esperado")
    private final String details;

    @Schema(description = "Timestamp do erro", example = "2025-05-24T13:33:00")
    private final String timestamp;

    public ErrorResponse(int status, String message, String details, String timestamp) {
        this.status = status;
        this.message = message;
        this.details = details;
        this.timestamp = timestamp;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public String getDetails() { return details; }
    public String getTimestamp() { return timestamp; }
}