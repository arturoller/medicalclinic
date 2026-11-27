package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record SearchVisitCommand(
        @Parameter(description = "Tylko terminy tego lekarza")
        @Positive Long doctorId,
        @Parameter(description = "Tylko terminy w tej placówce")
        @Positive Long institutionId,
        @Parameter(description = "Początek zakresu (włącznie), domyślnie teraz")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @Parameter(description = "Koniec zakresu (wyłącznie), domyślnie from + 30 dni")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
) {
}
