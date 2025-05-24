package com.example.api.proxy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Objeto para requisição de rastreamento")
public class TrackRequest {
    @Schema(description = "Código de rastreamento do pacote", example = "AB123456789BR")
    @NotNull(message = "Código de rastreamento é obrigatório")
    @Pattern(regexp = "[A-Z]{2}\\d{9}[A-Z]{2}", message = "Código de rastreamento deve seguir o formato XX999999999XX")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}