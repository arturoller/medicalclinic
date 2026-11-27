package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Termin wizyty")
public record VisitDto(
        @Schema(description = "ID terminu", example = "1")
        Long id,
        @Schema(description = "ID lekarza", example = "1")
        Long doctorId,
        @Schema(description = "ID placówki", example = "1")
        Long institutionId,
        @Schema(description = "ID pacjenta, który zarezerwował termin; null dla wolnego terminu", example = "1", nullable = true)
        Long patientId,
        @Schema(description = "Początek (czas serwera)", example = "2030-01-15T09:00:00")
        LocalDateTime startTime,
        @Schema(description = "Koniec (czas serwera)", example = "2030-01-15T09:30:00")
        LocalDateTime endTime,
        @Schema(description = "true, jeśli termin jest wolny (nikt go nie zarezerwował)", example = "false")
        boolean available
) {
}
