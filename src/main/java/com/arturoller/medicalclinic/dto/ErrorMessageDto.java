package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "Jednolity format błędu zwracany przez wszystkie endpointy")
public record ErrorMessageDto(
        @Schema(description = "Opis błędu", example = "Nie znaleziono użytkownika: 42")
        String message,
        @Schema(description = "Status HTTP (nazwa)", example = "NOT_FOUND", implementation = String.class)
        HttpStatus status
) {
}
