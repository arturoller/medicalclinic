package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Nowe dane placówki – nadpisują wszystkie dotychczasowe. Nazwa musi być unikalna.")
public record UpdateInstitutionCommand(
        @Schema(example = "Przychodnia Centrum")
        @NotBlank String name,
        @Schema(example = "Warszawa")
        @NotBlank String city,
        @Schema(description = "Kod pocztowy", example = "00-001")
        @NotBlank String postCode,
        @Schema(example = "Marszałkowska")
        @NotBlank String street,
        @Schema(description = "Numer budynku, może zawierać literę", example = "10A")
        @NotBlank String buildingNumber
) {
}
