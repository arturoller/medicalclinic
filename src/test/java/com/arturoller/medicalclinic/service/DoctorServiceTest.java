package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.DoctorMapper;
import com.arturoller.medicalclinic.mapper.InstitutionMapper;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Doctor;
import com.arturoller.medicalclinic.model.Institution;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.*;
import com.arturoller.medicalclinic.util.SortValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorServiceTest {
    private UserRepository userRepository;
    private PatientRepository patientRepository;
    private DoctorRepository doctorRepository;
    private InstitutionRepository institutionRepository;
    private VisitRepository visitRepository;
    private DoctorService doctorService;

    @BeforeEach
    void setup() {
        userRepository = mock(UserRepository.class);
        patientRepository = mock(PatientRepository.class);
        doctorRepository = mock(DoctorRepository.class);
        institutionRepository = mock(InstitutionRepository.class);
        visitRepository = mock(VisitRepository.class);
        doctorService = new DoctorService(userRepository, patientRepository, doctorRepository, institutionRepository, visitRepository,
                Mappers.getMapper(DoctorMapper.class), Mappers.getMapper(PatientMapper.class), Mappers.getMapper(InstitutionMapper.class),
                new SortValidator());
    }

    // ---------- list / getById ----------

    @Test
    void list_DoctorsExist_ReturnsPageOfDoctorDtos() {
        // given
        Pageable pageable = PageRequest.of(0, 2, Sort.by("lastName"));
        when(doctorRepository.findAll(pageable)).thenReturn(
                new PageImpl<>(List.of(doctor(1L, user(11L)), doctor(2L, user(12L))), pageable, 5));

        // when
        PageDto<DoctorDto> result = doctorService.list(pageable);

        // then
        assertAll(
                () -> assertEquals(2, result.content().size()),
                () -> assertEquals(1L, result.content().get(0).id()),
                () -> assertEquals(12L, result.content().get(1).userId()),
                () -> assertEquals(0, result.pageNumber()),
                () -> assertEquals(2, result.pageSize()),
                () -> assertEquals(5L, result.totalElements()),
                () -> assertEquals(3, result.totalPages()),
                () -> assertTrue(result.hasNext()),
                () -> assertFalse(result.isLast())
        );
    }

    @Test
    void list_SortByNotAllowedField_ThrowsInvalidSortFieldException() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("user.passwordHash"));

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class, () -> doctorService.list(pageable));

        // then
        assertEquals("Niedozwolone pole sortowania: user.passwordHash. Dozwolone pola: firstName, id, lastName", exception.getMessage());
        verifyNoInteractions(doctorRepository);
    }

    @Test
    void getById_DoctorExists_ReturnsDoctorDto() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L, user(11L))));

        // when
        DoctorDto result = doctorService.getById(1L);

        // then
        assertAll(
                () -> assertEquals(1L, result.id()),
                () -> assertEquals("Ewa", result.firstName()),
                () -> assertEquals("Zielińska", result.lastName()),
                () -> assertEquals("600100200", result.phoneNumber()),
                () -> assertEquals(11L, result.userId())
        );
    }

    @Test
    void getById_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.getById(1L));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
    }

    // ---------- save ----------

    @Test
    void save_UserExistsAndFree_CreatesDoctorLinkedToUser() {
        // given
        User user = user(5L);
        CreateDoctorCommand command = new CreateDoctorCommand(5L, "Marek", "Lewandowski", "601200300");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(doctorRepository.existsByUserId(5L)).thenReturn(false);
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        DoctorDto result = doctorService.save(command);

        // then
        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());
        assertAll(
                () -> assertSame(user, captor.getValue().getUser()),
                () -> assertEquals("Marek", result.firstName()),
                () -> assertEquals("Lewandowski", result.lastName()),
                () -> assertEquals("601200300", result.phoneNumber()),
                () -> assertEquals(5L, result.userId())
        );
    }

    @Test
    void save_UserNotFound_ThrowsUserNotFoundException() {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(5L, "Marek", "Lewandowski", "601200300");
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> doctorService.save(command));

        // then
        assertEquals("Nie znaleziono użytkownika: 5", exception.getMessage());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    void save_UserAlreadyDoctor_ThrowsUserAlreadyAssignedException() {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(5L, "Marek", "Lewandowski", "601200300");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L)));
        when(doctorRepository.existsByUserId(5L)).thenReturn(true);

        // when
        UserAlreadyAssignedException exception = assertThrows(UserAlreadyAssignedException.class, () -> doctorService.save(command));

        // then
        assertEquals("Użytkownik 5 jest już przypisany do innego lekarza", exception.getMessage());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    // ---------- update ----------

    @Test
    void update_DoctorExists_UpdatesAllFields() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        UpdateDoctorCommand command = new UpdateDoctorCommand("Ewa Maria", "Zielińska-Nowak", "600999999");
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(doctor)).thenReturn(doctor);

        // when
        DoctorDto result = doctorService.update(1L, command);

        // then
        assertAll(
                () -> assertEquals("Ewa Maria", result.firstName()),
                () -> assertEquals("Zielińska-Nowak", result.lastName()),
                () -> assertEquals("600999999", result.phoneNumber())
        );
        verify(doctorRepository).save(doctor);
    }

    @Test
    void update_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        UpdateDoctorCommand command = new UpdateDoctorCommand("Ewa Maria", "Zielińska-Nowak", "600999999");
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.update(1L, command));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    // ---------- delete ----------

    @Test
    void delete_DoctorWithoutAssignments_DeletesDoctor() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(visitRepository.existsByDoctorId(1L)).thenReturn(false);

        // when
        doctorService.delete(1L);

        // then
        verify(doctorRepository).delete(doctor);
    }

    @Test
    void delete_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.delete(1L));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verify(doctorRepository, never()).delete(any(Doctor.class));
    }

    @Test
    void delete_DoctorHasPatients_ThrowsDoctorAlreadyAssignedException() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        doctor.assignPatient(patient(2L, user(12L)));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        // when
        DoctorAlreadyAssignedException exception = assertThrows(DoctorAlreadyAssignedException.class, () -> doctorService.delete(1L));

        // then
        assertEquals("Lekarz 1 ma przypisanych pacjentów", exception.getMessage());
        verify(doctorRepository, never()).delete(any(Doctor.class));
    }

    @Test
    void delete_DoctorHasInstitutions_ThrowsDoctorAlreadyAssignedException() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        doctor.assignInstitution(institution(3L));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

        // when
        DoctorAlreadyAssignedException exception = assertThrows(DoctorAlreadyAssignedException.class, () -> doctorService.delete(1L));

        // then
        assertEquals("Lekarz 1 jest przypisany do placówek", exception.getMessage());
        verify(doctorRepository, never()).delete(any(Doctor.class));
    }

    @Test
    void delete_DoctorHasVisits_ThrowsDoctorAlreadyAssignedException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L, user(11L))));
        when(visitRepository.existsByDoctorId(1L)).thenReturn(true);

        // when
        DoctorAlreadyAssignedException exception = assertThrows(DoctorAlreadyAssignedException.class, () -> doctorService.delete(1L));

        // then
        assertEquals("Lekarz 1 ma wizyty", exception.getMessage());
        verify(doctorRepository, never()).delete(any(Doctor.class));
    }

    // ---------- assignPatient / unassignPatient / getPatients ----------

    @Test
    void assignPatient_NotAssignedYet_AssignsOnBothSides() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        Patient patient = patient(2L, user(12L));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

        // when
        doctorService.assignPatient(1L, 2L);

        // then
        assertAll(
                () -> assertTrue(doctor.hasPatient(patient)),
                () -> assertTrue(patient.getDoctors().contains(doctor))
        );
        verify(doctorRepository).save(doctor);
    }

    @Test
    void assignPatient_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.assignPatient(1L, 2L));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verifyNoInteractions(patientRepository);
    }

    @Test
    void assignPatient_PatientNotFound_ThrowsPatientNotFoundException() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.empty());

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> doctorService.assignPatient(1L, 2L));

        // then
        assertEquals("Nie znaleziono pacjenta: 2", exception.getMessage());
        assertTrue(doctor.getPatients().isEmpty());
    }

    @Test
    void assignPatient_AlreadyAssigned_ThrowsPatientAlreadyAssignedException() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        Patient patient = patient(2L, user(12L));
        doctor.assignPatient(patient);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

        // when
        PatientAlreadyAssignedException exception = assertThrows(PatientAlreadyAssignedException.class,
                () -> doctorService.assignPatient(1L, 2L));

        // then
        assertEquals("Pacjent 2 jest już przypisany do lekarza 1", exception.getMessage());
        assertEquals(1, doctor.getPatients().size());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    void unassignPatient_Assigned_UnassignsOnBothSides() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        Patient patient = patient(2L, user(12L));
        doctor.assignPatient(patient);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient));

        // when
        doctorService.unassignPatient(1L, 2L);

        // then
        assertAll(
                () -> assertFalse(doctor.hasPatient(patient)),
                () -> assertFalse(patient.getDoctors().contains(doctor))
        );
        verify(doctorRepository).save(doctor);
    }

    @Test
    void unassignPatient_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.unassignPatient(1L, 2L));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verifyNoInteractions(patientRepository);
    }

    @Test
    void unassignPatient_PatientNotFound_ThrowsPatientNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L, user(11L))));
        when(patientRepository.findById(2L)).thenReturn(Optional.empty());

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> doctorService.unassignPatient(1L, 2L));

        // then
        assertEquals("Nie znaleziono pacjenta: 2", exception.getMessage());
    }

    @Test
    void unassignPatient_NotAssigned_ThrowsPatientNotAssignedException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L, user(11L))));
        when(patientRepository.findById(2L)).thenReturn(Optional.of(patient(2L, user(12L))));

        // when
        PatientNotAssignedException exception = assertThrows(PatientNotAssignedException.class,
                () -> doctorService.unassignPatient(1L, 2L));

        // then
        assertEquals("Pacjent 2 nie jest przypisany do lekarza 1", exception.getMessage());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    void getPatients_DoctorExists_ReturnsPageOfAssignedPatients() {
        // given
        Pageable pageable = PageRequest.of(1, 1, Sort.by("lastName"));
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(patientRepository.findAllByDoctors_Id(any(), any())).thenReturn(
                new PageImpl<>(List.of(patient(2L, user(12L))), pageable, 3));

        // when
        PageDto<PatientDto> result = doctorService.getPatients(1L, pageable);

        // then
        verify(patientRepository).findAllByDoctors_Id(1L, pageable);
        assertAll(
                () -> assertEquals(1, result.content().size()),
                () -> assertEquals(2L, result.content().get(0).id()),
                () -> assertEquals(12L, result.content().get(0).userId()),
                () -> assertEquals(1, result.pageNumber()),
                () -> assertEquals(1, result.pageSize()),
                () -> assertEquals(3L, result.totalElements()),
                () -> assertEquals(3, result.totalPages()),
                () -> assertTrue(result.hasNext()),
                () -> assertFalse(result.isLast())
        );
    }

    @Test
    void getPatients_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.existsById(1L)).thenReturn(false);

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class,
                () -> doctorService.getPatients(1L, PageRequest.of(0, 10)));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verifyNoInteractions(patientRepository);
    }

    @Test
    void getPatients_SortByNotAllowedField_ThrowsInvalidSortFieldException() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("user.passwordHash"));
        when(doctorRepository.existsById(1L)).thenReturn(true);

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class,
                () -> doctorService.getPatients(1L, pageable));

        // then
        assertEquals("Niedozwolone pole sortowania: user.passwordHash. Dozwolone pola: birthday, firstName, id, lastName",
                exception.getMessage());
        verifyNoInteractions(patientRepository);
    }

    // ---------- assignInstitution / unassignInstitution / getInstitutions ----------

    @Test
    void assignInstitution_NotAssignedYet_AssignsOnBothSides() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        Institution institution = institution(3L);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(institutionRepository.findById(3L)).thenReturn(Optional.of(institution));

        // when
        doctorService.assignInstitution(1L, 3L);

        // then
        assertAll(
                () -> assertTrue(doctor.hasInstitution(institution)),
                () -> assertTrue(institution.getDoctors().contains(doctor))
        );
        verify(doctorRepository).save(doctor);
    }

    @Test
    void assignInstitution_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.assignInstitution(1L, 3L));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verifyNoInteractions(institutionRepository);
    }

    @Test
    void assignInstitution_InstitutionNotFound_ThrowsInstitutionNotFoundException() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(institutionRepository.findById(3L)).thenReturn(Optional.empty());

        // when
        InstitutionNotFoundException exception = assertThrows(InstitutionNotFoundException.class,
                () -> doctorService.assignInstitution(1L, 3L));

        // then
        assertEquals("Nie znaleziono placówki: 3", exception.getMessage());
        assertTrue(doctor.getInstitutions().isEmpty());
    }

    @Test
    void assignInstitution_AlreadyAssigned_ThrowsInstitutionAlreadyAssignedException() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        Institution institution = institution(3L);
        doctor.assignInstitution(institution);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(institutionRepository.findById(3L)).thenReturn(Optional.of(institution));

        // when
        InstitutionAlreadyAssignedException exception = assertThrows(InstitutionAlreadyAssignedException.class,
                () -> doctorService.assignInstitution(1L, 3L));

        // then
        assertEquals("Placówka 3 jest już przypisana do lekarza 1", exception.getMessage());
        assertEquals(1, doctor.getInstitutions().size());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    void unassignInstitution_Assigned_UnassignsOnBothSides() {
        // given
        Doctor doctor = doctor(1L, user(11L));
        Institution institution = institution(3L);
        doctor.assignInstitution(institution);
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(institutionRepository.findById(3L)).thenReturn(Optional.of(institution));

        // when
        doctorService.unassignInstitution(1L, 3L);

        // then
        assertAll(
                () -> assertFalse(doctor.hasInstitution(institution)),
                () -> assertFalse(institution.getDoctors().contains(doctor))
        );
        verify(doctorRepository).save(doctor);
    }

    @Test
    void unassignInstitution_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class, () -> doctorService.unassignInstitution(1L, 3L));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verifyNoInteractions(institutionRepository);
    }

    @Test
    void unassignInstitution_InstitutionNotFound_ThrowsInstitutionNotFoundException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L, user(11L))));
        when(institutionRepository.findById(3L)).thenReturn(Optional.empty());

        // when
        InstitutionNotFoundException exception = assertThrows(InstitutionNotFoundException.class,
                () -> doctorService.unassignInstitution(1L, 3L));

        // then
        assertEquals("Nie znaleziono placówki: 3", exception.getMessage());
    }

    @Test
    void unassignInstitution_NotAssigned_ThrowsInstitutionNotAssignedException() {
        // given
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor(1L, user(11L))));
        when(institutionRepository.findById(3L)).thenReturn(Optional.of(institution(3L)));

        // when
        InstitutionNotAssignedException exception = assertThrows(InstitutionNotAssignedException.class,
                () -> doctorService.unassignInstitution(1L, 3L));

        // then
        assertEquals("Placówka 3 nie jest przypisana do lekarza 1", exception.getMessage());
        verify(doctorRepository, never()).save(any(Doctor.class));
    }

    @Test
    void getInstitutions_DoctorExists_ReturnsPageOfAssignedInstitutions() {
        // given
        Pageable pageable = PageRequest.of(1, 1, Sort.by("name"));
        when(doctorRepository.existsById(1L)).thenReturn(true);
        when(institutionRepository.findAllByDoctors_Id(any(), any())).thenReturn(
                new PageImpl<>(List.of(institution(3L)), pageable, 3));

        // when
        PageDto<InstitutionDto> result = doctorService.getInstitutions(1L, pageable);

        // then
        verify(institutionRepository).findAllByDoctors_Id(1L, pageable);
        assertAll(
                () -> assertEquals(1, result.content().size()),
                () -> assertEquals(3L, result.content().get(0).id()),
                () -> assertEquals("Przychodnia Zdrowie", result.content().get(0).name()),
                () -> assertEquals(1, result.pageNumber()),
                () -> assertEquals(1, result.pageSize()),
                () -> assertEquals(3L, result.totalElements()),
                () -> assertEquals(3, result.totalPages()),
                () -> assertTrue(result.hasNext()),
                () -> assertFalse(result.isLast())
        );
    }

    @Test
    void getInstitutions_DoctorNotFound_ThrowsDoctorNotFoundException() {
        // given
        when(doctorRepository.existsById(1L)).thenReturn(false);

        // when
        DoctorNotFoundException exception = assertThrows(DoctorNotFoundException.class,
                () -> doctorService.getInstitutions(1L, PageRequest.of(0, 10)));

        // then
        assertEquals("Nie znaleziono lekarza: 1", exception.getMessage());
        verifyNoInteractions(institutionRepository);
    }

    @Test
    void getInstitutions_SortByNotAllowedField_ThrowsInvalidSortFieldException() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("doctors.user.passwordHash"));
        when(doctorRepository.existsById(1L)).thenReturn(true);

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class,
                () -> doctorService.getInstitutions(1L, pageable));

        // then
        assertEquals("Niedozwolone pole sortowania: doctors.user.passwordHash. Dozwolone pola: city, id, name",
                exception.getMessage());
        verifyNoInteractions(institutionRepository);
    }

    // Encje nie mają setId(), a from() zostawia id = null.
    private static User user(Long id) {
        User user = User.from(new CreateUserCommand("user" + id + "@example.com", "haslo123"), "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Doctor doctor(Long id, User user) {
        Doctor doctor = Doctor.from(new CreateDoctorCommand(user.getId(), "Ewa", "Zielińska", "600100200"));
        doctor.setUser(user);
        ReflectionTestUtils.setField(doctor, "id", id);
        return doctor;
    }

    private static Patient patient(Long id, User user) {
        Patient patient = Patient.from(new CreatePatientCommand(user.getId(), "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)));
        patient.setUser(user);
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }

    private static Institution institution(Long id) {
        Institution institution = Institution.from(new CreateInstitutionCommand("Przychodnia Zdrowie", "Warszawa", "00-950", "Marszałkowska", "10"));
        ReflectionTestUtils.setField(institution, "id", id);
        return institution;
    }
}
