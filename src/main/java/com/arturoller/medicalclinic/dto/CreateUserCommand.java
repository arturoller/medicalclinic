package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Nowe konto użytkownika")
public record CreateUserCommand(
        @Schema(description = "Unikalny adres e-mail", example = "jan.kowalski@example.com")
        @NotBlank @Email String email,
        @Schema(description = "Hasło, zapisywane jako hash BCrypt", format = "password", example = "Secret123!")
        @NotBlank String password
) {
}
