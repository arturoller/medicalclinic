package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.controller.doc.ApiBadRequest;
import com.arturoller.medicalclinic.controller.doc.ApiConflict;
import com.arturoller.medicalclinic.controller.doc.ApiNotFound;
import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Placówki", description = "Zarządzanie placówkami medycznymi")
@RestController
@RequestMapping("/institutions")
@RequiredArgsConstructor
public class InstitutionController {
    private final InstitutionService institutionService;

    @Operation(summary = "Lista placówek",
            description = "Zwraca placówki zapisane w systemie. Sortowanie: `id`, `name`, `city`.")
    @ApiBadRequest("Niedozwolone pole sortowania")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageDto<InstitutionDto> list(@ParameterObject @PageableDefault Pageable pageable) {
        return institutionService.list(pageable);
    }

    @Operation(summary = "Pobierz placówkę po ID")
    @ApiNotFound("Placówka nie istnieje")
    @GetMapping("/{institutionId}")
    @ResponseStatus(HttpStatus.OK)
    public InstitutionDto getById(@Parameter(description = "ID placówki", example = "1") @PathVariable Long institutionId) {
        return institutionService.getById(institutionId);
    }

    @Operation(summary = "Zaktualizuj dane placówki",
            description = "Nadpisuje nazwę i adres. Wszystkie pola są wymagane, nazwa musi być unikalna.")
    @ApiBadRequest("Brak wymaganego pola")
    @ApiNotFound("Placówka nie istnieje")
    @ApiConflict("Nazwa jest zajęta przez inną placówkę")
    @PutMapping("/{institutionId}")
    @ResponseStatus(HttpStatus.OK)
    public InstitutionDto update(@Parameter(description = "ID placówki", example = "1") @PathVariable Long institutionId,
                                 @Valid @RequestBody UpdateInstitutionCommand command) {
        return institutionService.update(institutionId, command);
    }

    @Operation(summary = "Usuń placówkę",
            description = "Placówkę można usunąć tylko wtedy, gdy nie ma przypisanych lekarzy ani wizyt.")
    @ApiNotFound("Placówka nie istnieje")
    @ApiConflict("Placówka ma przypisanych lekarzy albo wizyty")
    @DeleteMapping("/{institutionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID placówki", example = "1") @PathVariable Long institutionId) {
        institutionService.delete(institutionId);
    }

    @Operation(summary = "Utwórz placówkę",
            description = "Tworzy nową placówkę. Wszystkie pola są wymagane, nazwa musi być unikalna.")
    @ApiBadRequest("Brak wymaganego pola")
    @ApiConflict("Nazwa jest już zajęta")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstitutionDto create(@Valid @RequestBody CreateInstitutionCommand command) {
        return institutionService.save(command);
    }
}
