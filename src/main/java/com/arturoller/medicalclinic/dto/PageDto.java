package com.arturoller.medicalclinic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

@Schema(description = "Strona wyników")
public record PageDto<T>(
        @Schema(description = "Elementy na tej stronie")
        List<T> content,
        @Schema(description = "Numer strony (od 0)", example = "0")
        int pageNumber,
        @Schema(description = "Rozmiar strony", example = "10")
        int pageSize,
        @Schema(description = "Liczba wszystkich elementów", example = "42")
        long totalElements,
        @Schema(description = "Liczba wszystkich stron", example = "5")
        int totalPages,
        @Schema(description = "Czy istnieje następna strona", example = "true")
        boolean hasNext,
        @Schema(description = "Czy to ostatnia strona", example = "false")
        boolean isLast
) {
    public static <T> PageDto<T> from(Page<T> page) {
        return new PageDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.isLast()
        );
    }
}
