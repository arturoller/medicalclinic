package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.controller.doc.ApiBadRequest;
import com.arturoller.medicalclinic.controller.doc.ApiConflict;
import com.arturoller.medicalclinic.controller.doc.ApiNotFound;
import com.arturoller.medicalclinic.dto.CreateVisitCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.SearchVisitCommand;
import com.arturoller.medicalclinic.dto.VisitDto;
import com.arturoller.medicalclinic.service.VisitService;
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

@Tag(name = "Wizyty", description = "Terminy wizyt: tworzenie, wyszukiwanie, rezerwacja i odwoływanie")
@RestController
@RequestMapping("/visits")
@RequiredArgsConstructor
public class VisitController {
    private final VisitService visitService;

    @Operation(summary = "Wyszukaj terminy",
            description = "Wszystkie filtry są opcjonalne. Bez `from`/`to` zwraca terminy od teraz na 30 dni. "
                    + "Domyślnie posortowane po `startTime`. Sortowanie: `id`, `startTime`, `endTime`. "
                    + "Brak wyników to pusta strona, nie 404.")
    @ApiBadRequest("Niepoprawny format daty, ID ≤ 0 albo niedozwolone pole sortowania")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageDto<VisitDto> search(@Valid @ParameterObject SearchVisitCommand query,
                                    @ParameterObject @PageableDefault(sort = "startTime") Pageable pageable) {
        return visitService.search(query, pageable);
    }

    @Operation(summary = "Pobierz termin po ID")
    @ApiNotFound("Termin nie istnieje")
    @GetMapping("/{visitId}")
    @ResponseStatus(HttpStatus.OK)
    public VisitDto getById(@Parameter(description = "ID terminu", example = "1") @PathVariable Long visitId) {
        return visitService.getById(visitId);
    }

    @Operation(summary = "Usuń termin",
            description = "Można usunąć tylko termin, którego nikt nie zarezerwował.")
    @ApiNotFound("Termin nie istnieje")
    @ApiConflict("Termin jest zarezerwowany przez pacjenta")
    @DeleteMapping("/{visitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "ID terminu", example = "1") @PathVariable Long visitId) {
        visitService.delete(visitId);
    }

    @Operation(summary = "Dodaj termin",
            description = "Początek musi być przed końcem i w przyszłości, a obie godziny muszą wypadać na :00, :15, :30 lub :45. "
                    + "Lekarz musi pracować w podanej placówce.")
    @ApiBadRequest("Brak pól, koniec przed początkiem, termin w przeszłości albo godzina nie na pełny kwadrans")
    @ApiNotFound("Lekarz lub placówka nie istnieje")
    @ApiConflict("Lekarz nie pracuje w tej placówce albo termin nachodzi na inny jego termin")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitDto create(@Valid @RequestBody CreateVisitCommand command) {
        return visitService.create(command);
    }

    @Operation(summary = "Zarezerwuj termin",
            description = "Przypisuje pacjenta do wolnego terminu, który jeszcze się nie zaczął.")
    @ApiNotFound("Termin lub pacjent nie istnieje")
    @ApiConflict("Termin jest zajęty, już się zaczął albo pacjent ma w tym czasie inną wizytę")
    @PostMapping("/{visitId}/book/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bookPatient(@Parameter(description = "ID terminu", example = "1") @PathVariable Long visitId,
                            @Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId) {
        visitService.bookPatient(visitId, patientId);
    }

    @Operation(summary = "Odwołaj rezerwację",
            description = "Zwalnia termin zarezerwowany przez tego pacjenta, o ile termin jeszcze się nie zaczął.")
    @ApiNotFound("Termin nie istnieje")
    @ApiConflict("Termin nie jest zarezerwowany przez tego pacjenta albo już się zaczął")
    @DeleteMapping("/{visitId}/cancel/{patientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelPatient(@Parameter(description = "ID terminu", example = "1") @PathVariable Long visitId,
                              @Parameter(description = "ID pacjenta", example = "1") @PathVariable Long patientId) {
        visitService.cancelPatient(visitId, patientId);
    }
}
