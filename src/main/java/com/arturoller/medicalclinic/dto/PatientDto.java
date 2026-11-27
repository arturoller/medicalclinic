package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Pacjent")
public record PatientDto(
        @Schema(description = "ID pacjenta", example = "1") Long id,
        @Schema(example = "Jan")
        String firstName,
        @Schema(example = "Kowalski")
        String lastName,
        @Schema(example = "+48 600 200 300")
        String phoneNumber,
        @Schema(description = "Data urodzenia", example = "1990-05-17")
        LocalDate birthday,
        @Schema(description = "ID powiązanego użytkownika", example = "1")
        Long userId
) {
}
