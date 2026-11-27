package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Placówka medyczna")
public record InstitutionDto(
        @Schema(description = "ID placówki", example = "1")
        Long id,
        @Schema(example = "Przychodnia Centrum")
        String name,
        @Schema(example = "Warszawa")
        String city,
        @Schema(description = "Kod pocztowy", example = "00-001")
        String postCode,
        @Schema(example = "Marszałkowska")
        String street,
        @Schema(example = "10A")
        String buildingNumber
) {
}
