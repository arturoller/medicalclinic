package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Nowy lekarz powiązany z istniejącym użytkownikiem")
public record CreateDoctorCommand(
        @Schema(description = "ID istniejącego użytkownika", example = "2")
        @NotNull @Positive Long userId,
        @Schema(example = "Anna")
        @NotBlank String firstName,
        @Schema(example = "Nowak")
        @NotBlank String lastName,
        @Schema(example = "+48 600 100 200")
        @NotBlank String phoneNumber
) {
}
