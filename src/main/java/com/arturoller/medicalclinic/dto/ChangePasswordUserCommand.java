package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Zmiana hasła – wymaga podania aktualnego")
public record ChangePasswordUserCommand(
        @Schema(description = "Aktualne hasło", format = "password", example = "Secret123!")
        @NotBlank String currentPassword,
        @Schema(description = "Nowe hasło, inne niż aktualne", format = "password", example = "NewSecret456!")
        @NotBlank String newPassword
) {
}
