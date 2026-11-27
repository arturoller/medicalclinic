package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Użytkownik (hasło nigdy nie jest zwracane)")
public record UserDto(
        @Schema(description = "ID użytkownika", example = "1")
        Long id,
        @Schema(example = "jan.kowalski@example.com")
        String email
) {
}
