package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.controller.doc.ApiBadRequest;
import com.arturoller.medicalclinic.controller.doc.ApiConflict;
import com.arturoller.medicalclinic.controller.doc.ApiNotFound;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.service.PatientService;
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

@Tag(name = "Pacjenci", description = "Zarządzanie pacjentami")
@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @Operation(summary = "Lista pacjentów",
            description = "Zwraca pacjentów zapisanych w systemie. Sortowanie: `id`, `firstName`, `lastName`, `birthday`.")
    @ApiBadRequest("Niedozwolone pole sortowania")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageDto<PatientDto> list(@ParameterObject @PageableDefault Pageable pageable) {
        return patientService.list(pageable);
    }

    @Operation(summary = "Pobierz pacjenta po ID")
    @ApiNotFound("Pacjent nie istnieje")
    @GetMapping("/{patientId}")
    @ResponseStatus(HttpStatus.OK)
    public PatientDto getById(@Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId) {
        return patientService.getById(patientId);
    }

    @Operation(summary = "Zaktualizuj dane pacjenta",
            description = "Nadpisuje imię, nazwisko, numer telefonu i datę urodzenia. Wszystkie pola są wymagane.")
    @ApiBadRequest("Brak wymaganego pola albo niepoprawny format daty")
    @ApiNotFound("Pacjent nie istnieje")
    @PutMapping("/{patientId}")
    @ResponseStatus(HttpStatus.OK)
    public PatientDto update(@Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId,
                             @Valid @RequestBody UpdatePatientCommand command) {
        return patientService.update(patientId, command);
    }

    @Operation(summary = "Usuń pacjenta",
            description = "Pacjenta można usunąć tylko wtedy, gdy nie jest przypisany do lekarza i nie ma wizyt.")
    @ApiNotFound("Pacjent nie istnieje")
    @ApiConflict("Pacjent jest przypisany do lekarza albo ma wizyty")
    @DeleteMapping("/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId) {
        patientService.delete(patientId);
    }

    @Operation(summary = "Utwórz pacjenta",
            description = "Tworzy pacjenta dla istniejącego użytkownika (`userId`). Jeden użytkownik może mieć tylko jednego pacjenta.")
    @ApiBadRequest("Brak wymaganego pola, userId ≤ 0 albo niepoprawny format daty")
    @ApiNotFound("Użytkownik nie istnieje")
    @ApiConflict("Użytkownik ma już pacjenta")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDto create(@Valid @RequestBody CreatePatientCommand command) {
        return patientService.create(command);
    }
}
