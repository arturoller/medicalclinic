package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Nowe dane lekarza – nadpisują wszystkie dotychczasowe")
public record UpdateDoctorCommand(
        @Schema(example = "Anna")
        @NotBlank String firstName,
        @Schema(example = "Nowak")
        @NotBlank String lastName,
        @Schema(example = "+48 600 100 200")
        @NotBlank String phoneNumber
) {
}
