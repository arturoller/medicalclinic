package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Nowe dane pacjenta – nadpisują wszystkie dotychczasowe")
public record UpdatePatientCommand(
        @Schema(example = "Jan")
        @NotBlank String firstName,
        @Schema(example = "Kowalski")
        @NotBlank String lastName,
        @Schema(example = "+48 600 200 300")
        @NotBlank String phoneNumber,
        @Schema(description = "Data urodzenia", example = "1990-05-17")
        @NotNull LocalDate birthday
) {
}
