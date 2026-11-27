package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.VisitMapper;
import com.arturoller.medicalclinic.model.Doctor;
import com.arturoller.medicalclinic.model.Institution;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.Visit;
import com.arturoller.medicalclinic.repository.DoctorRepository;
import com.arturoller.medicalclinic.repository.InstitutionRepository;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.VisitRepository;
import com.arturoller.medicalclinic.util.SortValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VisitServiceTest {
    // "teraz" według zegara wstrzykniętego do serwisu
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 24, 10, 0);
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 25, 8, 0);
    private static final LocalDateTime END = START.plusMinutes(15);

    private VisitRepository visitRepository;
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;
    private InstitutionRepository institutionRepository;
    private VisitService visitService;

    @BeforeEach
    void setup() {
        visitRepository = mock(VisitRepository.class);
        patientRepository = mock(PatientRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        institutionRepository = mock(InstitutionRepository.class);
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        visitService = new VisitService(visitRepository, patientRepository, doctorRepository, institutionRepository,
                Mappers.getMapper(VisitMapper.class), clock, new SortValidator());
    }

    // ---------- create ----------

    @Test
    void create_ValidCommand_SavesAvailableVisit() {
        // given
        Doctor doctor = doctor(1L);
        Institution institution = institution(2L);
        doctor.assignInstitution(institution);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(institutionRepository.findById(2L)).thenReturn(Optional.of(institution));
        when(visitRepository.doctorHasOverlappingVisit(1L, START, END)).thenReturn(false);
        when(visitRepository.save(any(Visit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        VisitDto result = visitService.create(new CreateVisitCommand(1L, 2L, START, END));

        // then
        assertAll(
                () -> assertEquals(1L, result.doctorId()),
                () -> assertEquals(2L, result.institutionId()),
                () -> assertNull(result.patientId()),
                () -> assertEquals(START, result.startTime()),
                () -> assertEquals(END, result.endTime()),
                () -> assertTrue(result.available())
        );
        verify(visitRepository).save(any(Visit.class));
    }

    @ParameterizedTest(name = "koniec = start + {0} min")
    @ValueSource(longs = {0, -15})
    void create_EndNotAfterStart_ThrowsInvalidVisitTimeException(long endMinutesAfterStart) {
        // given
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, START.plusMinutes(endMinutesAfterStart));

        // when
        InvalidVisitTimeException exception = assertThrows(InvalidVisitTimeException.class, () -> visitService.create(command));

        // then
        assertEquals("Początek terminu musi być przed jego końcem", exception.getMessage());
        verifyNoInteractions(doctorRepository, institutionRepository, visitRepository);
    }

    @ParameterizedTest(name = "start = teraz + {0} min")
    @ValueSource(longs = {0, -60})
    void create_StartNotInFuture_ThrowsInvalidVisitTimeException(long startMinutesFromNow) {
        // given
        LocalDateTime start = NOW.plusMinutes(startMinutesFromNow);
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, start, start.plusMinutes(15));

        // when
        InvalidVisitTimeException exception = assertThrows(InvalidVisitTimeException.class, () -> visitService.create(command));

        // then
        assertEquals("Termin musi zaczynać się w przyszłości", exception.getMessage());
        verifyNoInteractions(doctorRepository, institutionRepository, visitRepository);
    }

    @ParameterizedTest(name = "start = {0}, koniec = {1}")
    @MethodSource("timesNotOnQuarter")
    void create_TimeNotOnQuarter_ThrowsInvalidVisitTimeException(LocalDateTime start, LocalDateTime end) {
        // given
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, start, end);

        // when
        InvalidVisitTimeException exception = assertThrows(InvalidVisitTimeException.class, () -> visitService.create(command));

        // then
        assertEquals("Termin musi zaczynać się i kończyć o :00, :15, :30 lub :45", exception.getMessage());
        verifyNoInteractions(doctorRepository, institutionRepository, visitRepository);
    }

    static Stream<Arguments> timesNotOnQuarter() {
        return Stream.of(
                Arguments.of(START.withMinute(10), START.withMinute(30)), // start 08:10
                Arguments.of(START, START.withMinute(20)),                // koniec 08:20
                Arguments.of(START.withSecond(30), END),                  // start 08:00:30
                Arguments.of(START, END.withNano(1))                      // koniec z nanosekundami
        );
    }

    @Test
    void create_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> visitService.create(command));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @Test
    void create_InstitutionNotFound_ThrowsInstitutionNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L)));
        when(institutionRepository.findById(2L)).thenReturn(Optional.empty());
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);

        // when
        InstitutionNotFoundException exception = assertThrows(InstitutionNotFoundException.class, () -> visitService.create(command));

        // then
        assertEquals("Nie znaleziono placówki: 2", exception.getMessage());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @Test
    void create_DoctorNotWorkingInInstitution_ThrowsInstitutionNotAssignedException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L)));
        when(institutionRepository.findById(2L)).thenReturn(Optional.of(institution(2L)));
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);

        // when
        InstitutionNotAssignedException exception = assertThrows(InstitutionNotAssignedException.class, () -> visitService.create(command));

        // then
        assertEquals("Placówka 2 nie jest przypisana do lekarza 1", exception.getMessage());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @Test
    void create_DoctorHasOverlappingVisit_ThrowsVisitOverlapException() {
        // given
        Doctor doctor = doctor(1L);
        Institution institution = institution(2L);
        doctor.assignInstitution(institution);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(institutionRepository.findById(2L)).thenReturn(Optional.of(institution));
        when(visitRepository.doctorHasOverlappingVisit(1L, START, END)).thenReturn(true);
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);

        // when
        VisitOverlapException exception = assertThrows(VisitOverlapException.class, () -> visitService.create(command));

        // then
        // między datami jest półpauza (–), nie zwykły myślnik
        assertEquals("Lekarz 1 ma już termin w przedziale 2026-11-25T08:00 – 2026-11-25T08:15", exception.getMessage());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    // ---------- getById ----------

    @Test
    void getById_BookedVisitExists_ReturnsVisitDto() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        visit.book(patient(3L));
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));

        // when
        VisitDto result = visitService.getById(10L);

        // then
        assertAll(
                () -> assertEquals(10L, result.id()),
                () -> assertEquals(1L, result.doctorId()),
                () -> assertEquals(2L, result.institutionId()),
                () -> assertEquals(3L, result.patientId()),
                () -> assertFalse(result.available())
        );
    }

    @Test
    void getById_VisitNotFound_ThrowsVisitNotFoundException() {
        // given
        when(visitRepository.findById(10L)).thenReturn(Optional.empty());

        // when
        VisitNotFoundException exception = assertThrows(VisitNotFoundException.class, () -> visitService.getById(10L));

        // then
        assertEquals("Nie znaleziono terminu: 10", exception.getMessage());
    }

    // ---------- search ----------

    @Test
    void search_AllFiltersGiven_PassesThemToRepositoryAndReturnsPage() {
        // given
        LocalDateTime from = LocalDateTime.of(2026, 12, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 12, 2, 0, 0);
        Pageable pageable = PageRequest.of(1, 10, Sort.by("id"));
        Visit visit = visit(10L, doctor(1L), institution(2L), from.plusHours(8));
        when(visitRepository.search(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(visit), pageable, 11));

        // when
        PageDto<VisitDto> result = visitService.search(new SearchVisitCommand(1L, 2L, from, to), pageable);

        // then
        verify(visitRepository).search(1L, 2L, from, to, pageable);
        assertAll(
                () -> assertEquals(1, result.content().size()),
                () -> assertEquals(10L, result.content().get(0).id()),
                () -> assertEquals(1L, result.content().get(0).doctorId()),
                () -> assertEquals(2L, result.content().get(0).institutionId()),
                () -> assertEquals(1, result.pageNumber()),
                () -> assertEquals(10, result.pageSize()),
                () -> assertEquals(11L, result.totalElements()),
                () -> assertEquals(2, result.totalPages()),
                () -> assertFalse(result.hasNext()),
                () -> assertTrue(result.isLast())
        );
    }

    @Test
    void search_NoDates_SearchesFromNowFor30Days() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty(pageable));

        // when
        visitService.search(new SearchVisitCommand(null, null, null, null), pageable);

        // then
        verify(visitRepository).search(null, null, NOW, NOW.plusDays(30), pageable);
    }

    @Test
    void search_OnlyFromGiven_SearchesFromGivenDateFor30Days() {
        // given
        LocalDateTime from = LocalDateTime.of(2026, 12, 1, 0, 0);
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty(pageable));

        // when
        visitService.search(new SearchVisitCommand(null, null, from, null), pageable);

        // then
        verify(visitRepository).search(null, null, from, from.plusDays(30), pageable);
    }

    @Test
    void search_OnlyToGiven_SearchesFromNowToGivenDate() {
        // given
        LocalDateTime to = LocalDateTime.of(2026, 12, 1, 0, 0);
        Pageable pageable = PageRequest.of(0, 10);
        when(visitRepository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty(pageable));

        // when
        visitService.search(new SearchVisitCommand(null, null, null, to), pageable);

        // then
        verify(visitRepository).search(null, null, NOW, to, pageable);
    }

    @ParameterizedTest(name = "sortowanie po {0}")
    @ValueSource(strings = {"id", "startTime", "endTime"})
    void search_SortByAllowedField_PassesSortToRepository(String field) {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(field));
        when(visitRepository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty(pageable));

        // when
        visitService.search(new SearchVisitCommand(null, null, null, null), pageable);

        // then
        verify(visitRepository).search(null, null, NOW, NOW.plusDays(30), pageable);
    }

    @Test
    void search_SortByNotAllowedField_ThrowsInvalidSortFieldException() {
        // given
        // doctorId jest w VisitDto, ale encja Visit ma tylko doctor.id – sortowanie po nim nie jest dozwolone
        Pageable pageable = PageRequest.of(0, 10, Sort.by("doctorId"));
        SearchVisitCommand query = new SearchVisitCommand(null, null, null, null);

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class, () -> visitService.search(query, pageable));

        // then
        assertEquals("Niedozwolone pole sortowania: doctorId. Dozwolone pola: endTime, id, startTime", exception.getMessage());
        verifyNoInteractions(visitRepository);
    }

    // ---------- delete ----------

    @Test
    void delete_FreeVisit_DeletesVisit() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));

        // when
        visitService.delete(10L);

        // then
        verify(visitRepository).delete(visit);
    }

    @Test
    void delete_VisitNotFound_ThrowsVisitNotFoundException() {
        // given
        when(visitRepository.findById(10L)).thenReturn(Optional.empty());

        // when
        VisitNotFoundException exception = assertThrows(VisitNotFoundException.class, () -> visitService.delete(10L));

        // then
        assertEquals("Nie znaleziono terminu: 10", exception.getMessage());
        verify(visitRepository, never()).delete(any(Visit.class));
    }

    @Test
    void delete_BookedVisit_ThrowsPatientAlreadyAssignedException() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        visit.book(patient(3L));
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));

        // when
        PatientAlreadyAssignedException exception = assertThrows(PatientAlreadyAssignedException.class, () -> visitService.delete(10L));

        // then
        assertEquals("Termin 10 jest zarezerwowany przez pacjenta 3", exception.getMessage());
        verify(visitRepository, never()).delete(any(Visit.class));
    }

    // ---------- bookPatient ----------

    @Test
    void bookPatient_FreeFutureVisit_BooksPatient() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        Patient patient = patient(3L);
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(3L)).thenReturn(Optional.of(patient));
        when(visitRepository.patientHasOverlappingVisit(3L, START, END)).thenReturn(false);

        // when
        visitService.bookPatient(10L, 3L);

        // then
        assertAll(
                () -> assertSame(patient, visit.getPatient()),
                () -> assertFalse(visit.isAvailable())
        );
        verify(visitRepository).save(visit);
    }

    @Test
    void bookPatient_VisitNotFound_ThrowsVisitNotFoundException() {
        // given
        when(visitRepository.findById(10L)).thenReturn(Optional.empty());
        Long patientId = 3L;

        // when
        VisitNotFoundException exception = assertThrows(VisitNotFoundException.class, () -> visitService.bookPatient(10L, patientId));

        // then
        assertEquals("Nie znaleziono terminu: 10", exception.getMessage());
        verifyNoInteractions(patientRepository);
    }

    @Test
    void bookPatient_PatientNotFound_ThrowsPatientNotFoundException() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(3L)).thenReturn(Optional.empty());
        Long patientId = 3L;

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> visitService.bookPatient(10L, patientId));

        // then
        assertEquals("Nie znaleziono pacjenta: 3", exception.getMessage());
        assertTrue(visit.isAvailable());
    }

    @Test
    void bookPatient_VisitAlreadyBooked_ThrowsVisitAlreadyBookedException() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        visit.book(patient(4L));
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(3L)).thenReturn(Optional.of(patient(3L)));
        Long patientId = 3L;

        // when
        VisitAlreadyBookedException exception = assertThrows(VisitAlreadyBookedException.class, () -> visitService.bookPatient(10L, patientId));

        // then
        assertEquals("Termin 10 jest już zarezerwowany", exception.getMessage());
        assertEquals(4L, visit.getPatient().getId());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @ParameterizedTest(name = "start wizyty = teraz + {0} min")
    @ValueSource(longs = {0, -15})
    void bookPatient_VisitAlreadyStarted_ThrowsVisitAlreadyStartedException(long startMinutesFromNow) {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), NOW.plusMinutes(startMinutesFromNow));
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(3L)).thenReturn(Optional.of(patient(3L)));
        Long patientId = 3L;

        // when
        VisitAlreadyStartedException exception = assertThrows(VisitAlreadyStartedException.class, () -> visitService.bookPatient(10L, patientId));

        // then
        assertEquals("Termin 10 już się rozpoczął", exception.getMessage());
        assertTrue(visit.isAvailable());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @Test
    void bookPatient_PatientHasOverlappingVisit_ThrowsVisitOverlapException() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        when(patientRepository.findById(3L)).thenReturn(Optional.of(patient(3L)));
        when(visitRepository.patientHasOverlappingVisit(3L, START, END)).thenReturn(true);
        Long patientId = 3L;

        // when
        VisitOverlapException exception = assertThrows(VisitOverlapException.class, () -> visitService.bookPatient(10L, patientId));

        // then
        assertEquals("Pacjent 3 ma w tym czasie inną wizytę", exception.getMessage());
        assertTrue(visit.isAvailable());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    // ---------- cancelPatient ----------

    @Test
    void cancelPatient_BookedBySamePatient_CancelsBooking() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        visit.book(patient(3L));
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));

        // when
        visitService.cancelPatient(10L, 3L);

        // then
        assertAll(
                () -> assertNull(visit.getPatient()),
                () -> assertTrue(visit.isAvailable())
        );
        verify(visitRepository).save(visit);
    }

    @Test
    void cancelPatient_VisitNotFound_ThrowsVisitNotFoundException() {
        // given
        when(visitRepository.findById(10L)).thenReturn(Optional.empty());
        Long patientId = 3L;

        // when
        VisitNotFoundException exception = assertThrows(VisitNotFoundException.class, () -> visitService.cancelPatient(10L, patientId));

        // then
        assertEquals("Nie znaleziono terminu: 10", exception.getMessage());
    }

    @Test
    void cancelPatient_VisitNotBooked_ThrowsVisitNotBookedByPatientException() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        Long patientId = 3L;

        // when
        VisitNotBookedByPatientException exception = assertThrows(VisitNotBookedByPatientException.class,
                () -> visitService.cancelPatient(10L, patientId));

        // then
        assertEquals("Termin 10 nie jest zarezerwowany przez pacjenta 3", exception.getMessage());
    }

    @Test
    void cancelPatient_BookedByAnotherPatient_ThrowsVisitNotBookedByPatientException() {
        // given
        Visit visit = visit(10L, doctor(1L), institution(2L), START);
        visit.book(patient(4L));
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        Long patientId = 3L;

        // when
        VisitNotBookedByPatientException exception = assertThrows(VisitNotBookedByPatientException.class,
                () -> visitService.cancelPatient(10L, patientId));

        // then
        assertEquals("Termin 10 nie jest zarezerwowany przez pacjenta 3", exception.getMessage());
        assertEquals(4L, visit.getPatient().getId());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    @ParameterizedTest(name = "start wizyty = teraz + {0} min")
    @ValueSource(longs = {0, -15})
    void cancelPatient_VisitAlreadyStarted_ThrowsVisitAlreadyStartedException(long startMinutesFromNow) {
        // given
        Patient patient = patient(3L);
        Visit visit = visit(10L, doctor(1L), institution(2L), NOW.plusMinutes(startMinutesFromNow));
        visit.book(patient);
        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));
        Long patientId = 3L;

        // when
        VisitAlreadyStartedException exception = assertThrows(VisitAlreadyStartedException.class,
                () -> visitService.cancelPatient(10L, patientId));

        // then
        assertEquals("Termin 10 już się rozpoczął", exception.getMessage());
        assertSame(patient, visit.getPatient());
        verify(visitRepository, never()).save(any(Visit.class));
    }

    // Encje nie mają setId(), a from() zostawia id = null.
    private static Doctor doctor(Long id) {
        Doctor doctor = Doctor.from(new CreateDoctorCommand(null, "Ewa", "Zielińska", "600100200"));
        ReflectionTestUtils.setField(doctor, "id", id);
        return doctor;
    }

    private static Institution institution(Long id) {
        Institution institution = Institution.from(new CreateInstitutionCommand("Przychodnia Zdrowie", "Warszawa", "00-950", "Marszałkowska", "10"));
        ReflectionTestUtils.setField(institution, "id", id);
        return institution;
    }

    private static Patient patient(Long id) {
        Patient patient = Patient.from(new CreatePatientCommand(null, "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)));
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }

    private static Visit visit(Long id, Doctor doctor, Institution institution, LocalDateTime start) {
        Visit visit = Visit.create(doctor, institution, start, start.plusMinutes(15));
        ReflectionTestUtils.setField(visit, "id", id);
        return visit;
    }
}
