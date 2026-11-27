package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.controller.doc.ApiBadRequest;
import com.arturoller.medicalclinic.controller.doc.ApiConflict;
import com.arturoller.medicalclinic.controller.doc.ApiNotFound;
import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.service.DoctorService;
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

@Tag(name = "Lekarze", description = "Zarządzanie lekarzami oraz ich przypisaniami do pacjentów i placówek")
@RestController
@RequestMapping("/doctors")
@RequiredArgsConstructor
public class DoctorController {
    private final DoctorService doctorService;

    @Operation(summary = "Lista lekarzy",
            description = "Zwraca lekarzy zapisanych w systemie. Sortowanie: `id`, `firstName`, `lastName`.")
    @ApiBadRequest("Niedozwolone pole sortowania")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageDto<DoctorDto> list(@ParameterObject @PageableDefault Pageable pageable) {
        return doctorService.list(pageable);
    }

    @Operation(summary = "Pobierz lekarza po ID")
    @ApiNotFound("Lekarz nie istnieje")
    @GetMapping("/{doctorId}")
    @ResponseStatus(HttpStatus.OK)
    public DoctorDto getById(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId) {
        return doctorService.getById(doctorId);
    }

    @Operation(summary = "Zaktualizuj dane lekarza",
            description = "Nadpisuje imię, nazwisko i numer telefonu. Wszystkie pola są wymagane.")
    @ApiBadRequest("Brak wymaganego pola")
    @ApiNotFound("Lekarz nie istnieje")
    @PutMapping("/{doctorId}")
    @ResponseStatus(HttpStatus.OK)
    public DoctorDto update(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                            @Valid @RequestBody UpdateDoctorCommand command) {
        return doctorService.update(doctorId, command);
    }

    @Operation(summary = "Usuń lekarza",
            description = "Lekarza można usunąć tylko wtedy, gdy nie ma przypisanych pacjentów, placówek ani wizyt.")
    @ApiNotFound("Lekarz nie istnieje")
    @ApiConflict("Lekarz ma przypisanych pacjentów, placówki albo wizyty")
    @DeleteMapping("/{doctorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId) {
        doctorService.delete(doctorId);
    }

    @Operation(summary = "Utwórz lekarza",
            description = "Tworzy lekarza powiązanego z istniejącym użytkownikiem (`userId`). Jeden użytkownik może być tylko jednym lekarzem.")
    @ApiBadRequest("Brak wymaganego pola albo userId ≤ 0")
    @ApiNotFound("Użytkownik nie istnieje")
    @ApiConflict("Użytkownik jest już lekarzem")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorDto create(@Valid @RequestBody CreateDoctorCommand command) {
        return doctorService.save(command);
    }

    @Operation(summary = "Lista pacjentów lekarza",
            description = "Zwraca pacjentów przypisanych do lekarza. Sortowanie: `id`, `firstName`, `lastName`, `birthday`.")
    @ApiBadRequest("Niedozwolone pole sortowania")
    @ApiNotFound("Lekarz nie istnieje")
    @GetMapping("/{doctorId}/patients")
    @ResponseStatus(HttpStatus.OK)
    public PageDto<PatientDto> getPatients(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                                        @ParameterObject @PageableDefault Pageable pageable) {
        return doctorService.getPatients(doctorId, pageable);
    }

    @Operation(summary = "Przypisz pacjenta do lekarza")
    @ApiNotFound("Lekarz lub pacjent nie istnieje")
    @ApiConflict("Pacjent jest już przypisany do tego lekarza")
    @PostMapping("/{doctorId}/patients/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPatient(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                              @Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId) {
        doctorService.assignPatient(doctorId, patientId);
    }

    @Operation(summary = "Odepnij pacjenta od lekarza",
            description = "Usuwa tylko powiązanie – lekarz i pacjent zostają w systemie.")
    @ApiNotFound("Lekarz lub pacjent nie istnieje")
    @ApiConflict("Pacjent nie jest przypisany do tego lekarza")
    @DeleteMapping("/{doctorId}/patients/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignPatient(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                                @Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId) {
        doctorService.unassignPatient(doctorId, patientId);
    }

    @Operation(summary = "Lista placówek lekarza",
            description = "Zwraca placówki, w których pracuje lekarz. Sortowanie: `id`, `name`, `city`.")
    @ApiBadRequest("Niedozwolone pole sortowania")
    @ApiNotFound("Lekarz nie istnieje")
    @GetMapping("/{doctorId}/institutions")
    @ResponseStatus(HttpStatus.OK)
    public PageDto<InstitutionDto> getInstitutions(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                                                @ParameterObject @PageableDefault Pageable pageable) {
        return doctorService.getInstitutions(doctorId, pageable);
    }

    @Operation(summary = "Przypisz lekarza do placówki")
    @ApiNotFound("Lekarz lub placówka nie istnieje")
    @ApiConflict("Lekarz jest już przypisany do tej placówki")
    @PostMapping("/{doctorId}/institutions/{institutionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignInstitution(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                                  @Parameter(description = "ID placówki", example = "1") @PathVariable Long institutionId) {
        doctorService.assignInstitution(doctorId, institutionId);
    }

    @Operation(summary = "Odepnij lekarza od placówki",
            description = "Usuwa tylko powiązanie – lekarz i placówka zostają w systemie.")
    @ApiNotFound("Lekarz lub placówka nie istnieje")
    @ApiConflict("Lekarz nie jest przypisany do tej placówki")
    @DeleteMapping("/{doctorId}/institutions/{institutionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignInstitution(@Parameter(description = "ID lekarza", example = "1") @PathVariable Long doctorId,
                                    @Parameter(description = "ID placówki", example = "1") @PathVariable Long institutionId) {
        doctorService.unassignInstitution(doctorId, institutionId);
    }
}
