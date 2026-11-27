package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lekarz")
public record DoctorDto(
        @Schema(description = "ID lekarza", example = "1")
        Long id,
        @Schema(example = "Anna")
        String firstName,
        @Schema(example = "Nowak")
        String lastName,
        @Schema(example = "+48 600 100 200")
        String phoneNumber,
        @Schema(description = "ID powiązanego użytkownika", example = "2")
        Long userId
) {
}
