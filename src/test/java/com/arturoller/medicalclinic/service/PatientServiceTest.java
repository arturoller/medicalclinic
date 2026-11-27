package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.CreateDoctorCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.InvalidSortFieldException;
import com.arturoller.medicalclinic.exception.PatientAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.PatientNotFoundException;
import com.arturoller.medicalclinic.exception.UserAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.UserNotFoundException;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Doctor;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.UserRepository;
import com.arturoller.medicalclinic.repository.VisitRepository;
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

class PatientServiceTest {
    private PatientRepository patientRepository;
    private UserRepository userRepository;
    private VisitRepository visitRepository;
    private PatientService patientService;

    @BeforeEach
    void setup() {
        patientRepository = mock(PatientRepository.class);
        userRepository = mock(UserRepository.class);
        visitRepository = mock(VisitRepository.class);
        patientService = new PatientService(patientRepository, userRepository, visitRepository,
                Mappers.getMapper(PatientMapper.class), new SortValidator());
    }

    @Test
    void list_PatientsExist_ReturnsPageOfPatientDtos() {
        // given
        Pageable pageable = PageRequest.of(0, 2);
        when(patientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                patient(1L, user(11L)),
                patient(2L, user(12L))), pageable, 3));

        // when
        PageDto<PatientDto> result = patientService.list(pageable);

        // then
        assertAll(
                () -> assertEquals(2, result.content().size()),
                () -> assertEquals(1L, result.content().get(0).id()),
                () -> assertEquals("Kowalski", result.content().get(0).lastName()),
                () -> assertEquals(12L, result.content().get(1).userId()),
                () -> assertEquals(0, result.pageNumber()),
                () -> assertEquals(2, result.pageSize()),
                () -> assertEquals(3L, result.totalElements()),
                () -> assertEquals(2, result.totalPages()),
                () -> assertTrue(result.hasNext()),
                () -> assertFalse(result.isLast())
        );
    }

    @Test
    void list_InvalidSortField_ThrowsInvalidSortFieldException() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("phoneNumber"));

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class,
                () -> patientService.list(pageable));

        // then
        // kolejność dozwolonych pól pochodzi z Set.of(), więc sprawdzamy tylko początek komunikatu
        assertTrue(exception.getMessage().startsWith("Niedozwolone pole sortowania: phoneNumber. Dozwolone pola: "),
                exception.getMessage());
        verify(patientRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void list_ValidSortField_PassesSortToRepository() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("lastName"));
        when(patientRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                patient(1L, user(11L))), pageable, 1));

        // when
        PageDto<PatientDto> result = patientService.list(pageable);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).findAll(captor.capture());
        assertAll(
                () -> assertEquals(Sort.by("lastName"), captor.getValue().getSort()),
                () -> assertEquals(1, result.content().size()),
                () -> assertEquals(1L, result.content().get(0).id())
        );
    }

    @Test
    void getById_PatientExists_ReturnsPatientDto() {
        // given
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient(1L, user(11L))));

        // when
        PatientDto result = patientService.getById(1L);

        // then
        assertAll(
                () -> assertEquals(1L, result.id()),
                () -> assertEquals("Jan", result.firstName()),
                () -> assertEquals("Kowalski", result.lastName()),
                () -> assertEquals("500100200", result.phoneNumber()),
                () -> assertEquals(LocalDate.of(1985, 3, 12), result.birthday()),
                () -> assertEquals(11L, result.userId())
        );
    }

    @Test
    void getById_PatientNotFound_ThrowsPatientNotFoundException() {
        // given
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> patientService.getById(1L));

        // then
        assertEquals("Nie znaleziono pacjenta: 1", exception.getMessage());
    }

    @Test
    void create_UserExistsAndFree_CreatesPatientLinkedToUser() {
        // given
        User user = user(5L);
        CreatePatientCommand command = new CreatePatientCommand(5L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(patientRepository.existsByUserId(5L)).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PatientDto result = patientService.create(command);

        // then
        ArgumentCaptor<Patient> captor = ArgumentCaptor.forClass(Patient.class);
        verify(patientRepository).save(captor.capture());
        assertAll(
                () -> assertSame(user, captor.getValue().getUser()),
                () -> assertEquals("Anna", result.firstName()),
                () -> assertEquals("Nowak", result.lastName()),
                () -> assertEquals("501200300", result.phoneNumber()),
                () -> assertEquals(LocalDate.of(1992, 7, 24), result.birthday()),
                () -> assertEquals(5L, result.userId())
        );
    }

    @Test
    void create_UserNotFound_ThrowsUserNotFoundException() {
        // given
        CreatePatientCommand command = new CreatePatientCommand(5L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        // when
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> patientService.create(command));

        // then
        assertEquals("Nie znaleziono użytkownika: 5", exception.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void create_UserAlreadyHasPatient_ThrowsUserAlreadyAssignedException() {
        // given
        CreatePatientCommand command = new CreatePatientCommand(5L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        when(userRepository.findById(5L)).thenReturn(Optional.of(user(5L)));
        when(patientRepository.existsByUserId(5L)).thenReturn(true);

        // when
        UserAlreadyAssignedException exception = assertThrows(UserAlreadyAssignedException.class, () -> patientService.create(command));

        // then
        assertEquals("Użytkownik 5 jest już przypisany do innego pacjenta", exception.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void update_PatientExists_UpdatesAllFields() {
        // given
        Patient patient = patient(1L, user(11L));
        UpdatePatientCommand command = new UpdatePatientCommand("Jan Kuba", "Kowalski-Nowak", "500678200", LocalDate.of(1985, 3, 13));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenReturn(patient);

        // when
        PatientDto result = patientService.update(1L, command);

        // then
        assertAll(
                () -> assertEquals("Jan Kuba", result.firstName()),
                () -> assertEquals("Kowalski-Nowak", result.lastName()),
                () -> assertEquals("500678200", result.phoneNumber()),
                () -> assertEquals(LocalDate.of(1985, 3, 13), result.birthday())
        );
        verify(patientRepository).save(patient);
    }

    @Test
    void update_PatientNotFound_ThrowsPatientNotFoundException() {
        // given
        UpdatePatientCommand command = new UpdatePatientCommand("Jan Kuba", "Kowalski", "500678200", LocalDate.of(1985, 3, 13));
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> patientService.update(1L, command));

        // then
        assertEquals("Nie znaleziono pacjenta: 1", exception.getMessage());
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    void delete_PatientWithoutDoctorsAndVisits_DeletesPatient() {
        // given
        Patient patient = patient(1L, user(11L));
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(visitRepository.existsByPatientId(1L)).thenReturn(false);

        // when
        patientService.delete(1L);

        // then
        verify(patientRepository).delete(patient);
    }

    @Test
    void delete_PatientNotFound_ThrowsPatientNotFoundException() {
        // given
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> patientService.delete(1L));

        // then
        assertEquals("Nie znaleziono pacjenta: 1", exception.getMessage());
        verify(patientRepository, never()).delete(any(Patient.class));
    }

    @Test
    void delete_PatientAssignedToDoctor_ThrowsPatientAlreadyAssignedException() {
        // given
        Patient patient = patient(1L, user(11L));
        Doctor doctor = Doctor.from(new CreateDoctorCommand(12L, "Ewa", "Zielińska", "600100200"));
        doctor.assignPatient(patient);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

        // when
        PatientAlreadyAssignedException exception = assertThrows(PatientAlreadyAssignedException.class, () -> patientService.delete(1L));

        // then
        assertEquals("Pacjent 1 jest przypisany do lekarza", exception.getMessage());
        verify(patientRepository, never()).delete(any(Patient.class));
    }

    @Test
    void delete_PatientHasVisits_ThrowsPatientAlreadyAssignedException() {
        // given
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient(1L, user(11L))));
        when(visitRepository.existsByPatientId(1L)).thenReturn(true);

        // when
        PatientAlreadyAssignedException exception = assertThrows(PatientAlreadyAssignedException.class, () -> patientService.delete(1L));

        // then
        assertEquals("Pacjent 1 ma wizyty", exception.getMessage());
        verify(patientRepository, never()).delete(any(Patient.class));
    }

    // Encje nie mają setId(), a from() zostawia id = null.
    private static User user(Long id) {
        User user = User.from(new CreateUserCommand("user" + id + "@example.com", "haslo123"), "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private static Patient patient(Long id, User user) {
        Patient patient = Patient.from(new CreatePatientCommand(user.getId(), "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)));
        patient.setUser(user);
        ReflectionTestUtils.setField(patient, "id", id);
        return patient;
    }
}
