package com.arturoller.medicalclinic.dto;

import org.springframework.http.HttpStatus;

public record ErrorMessageDto(
        String message,
        HttpStatus status
) {
}
