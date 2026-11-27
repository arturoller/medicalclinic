package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

@Schema(description = "Nowy termin. Początek i koniec na pełny kwadrans (:00/:15/:30/:45), początek w przyszłości.")
public record CreateVisitCommand(
        @Schema(description = "ID lekarza", example = "1")
        @NotNull @Positive Long doctorId,
        @Schema(description = "ID placówki, w której lekarz pracuje", example = "1")
        @NotNull @Positive Long institutionId,
        @Schema(description = "Początek (czas serwera)", example = "2030-01-15T09:00:00")
        @NotNull LocalDateTime startTime,
        @Schema(description = "Koniec, później niż początek", example = "2030-01-15T09:30:00")
        @NotNull LocalDateTime endTime
) {
}
