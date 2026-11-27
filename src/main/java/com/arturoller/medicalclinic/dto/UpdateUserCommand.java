package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Nowy adres e-mail użytkownika")
public record UpdateUserCommand(
        @Schema(description = "Unikalny adres e-mail", example = "jan.nowy@example.com")
        @NotBlank @Email String email
) {
}
