package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.config.SecurityConfig;
import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.PatientCurrentPasswordIsTheSame;
import com.arturoller.medicalclinic.exception.PatientNotFoundException;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PatientServiceTest {
    private PatientRepository patientRepository;
    private PatientMapper patientMapper;
    private PasswordEncoder passwordEncoder;
    private PatientService patientService;

    @BeforeEach
    public void setup() {
        this.patientRepository = Mockito.mock(PatientRepository.class);
        this.patientMapper = Mockito.mock(PatientMapper.class);
        this.passwordEncoder = new SecurityConfig().passwordEncoder();
        this.patientService = new PatientService(patientRepository, patientMapper, passwordEncoder);
    }

    @Test
    void getAll_PatientsExists_PatientsReturned() {
        // given
        List<Patient> patients = List.of(Patient.from(new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123")), Patient.from(new CreatePatientCommand(new Email("anna.nowak@example.com"), "haslo123", "ABC123456", "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24)), passwordEncoder.encode("haslo123")));
        when(patientRepository.findAll()).thenReturn(patients);

        // when
        List<Patient> result = patientService.getAll();

        // then
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals("Jan", result.get(0).getFirstName()),
                () -> assertEquals("Nowak", result.get(1).getLastName()),
                () -> assertInstanceOf(Email.class, result.get(0).getEmail()),
                () -> assertTrue(passwordEncoder.matches("haslo123", result.get(1).getPasswordHash()))
        );
    }

    @Test
    void finByEmail_PatientExists_PatientsReturned() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(new CreatePatientCommand(email, "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123"));
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        // when
        Patient result = patientRepository.findByEmail(email).orElseThrow();

        // then
        assertAll(
                () -> assertEquals("Jan", result.getFirstName()),
                () -> assertEquals("Kowalski", result.getLastName()),
                () -> assertInstanceOf(Email.class, result.getEmail()),
                () -> assertTrue(passwordEncoder.matches("haslo123", result.getPasswordHash()))
        );
        verify(patientRepository, times(1)).findByEmail(eq(email));
    }

    @Test
    void findByEmail_PatientNotExists_ThrowsException() {
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(new CreatePatientCommand(email, "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123"));
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));
    }

    @Test
    void create_CreatePatient_ReturnPatientDto() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        CreatePatientCommand command = new CreatePatientCommand(email, "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12));
        when(patientRepository.exists(email)).thenReturn(false);

        Patient patient = Patient.from(command, passwordEncoder.encode("haslo123"));
        when(patientMapper.toModel(command)).thenReturn(patient);
        when(patientRepository.create(any(Patient.class))).thenReturn(patient);

        PatientDto exceptedDto = new PatientDto("jan.kowalski@example.com", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12));
        when(patientMapper.toDto(any(Patient.class))).thenReturn(exceptedDto);

        // when
        PatientDto result = patientService.create(command);

        // then
        assertAll(() -> assertEquals("Jan", result.firstName()), () -> assertEquals("jan.kowalski@example.com", result.email()), () -> assertEquals("500100200", result.phoneNumber()));
        verify(patientRepository, times(1)).create(any(Patient.class));
    }

    @Test
    void update_UpdatePatient_ReturnPatientDto() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(new CreatePatientCommand(email, "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123"));
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        PatientDto exceptedDto = new PatientDto("jan.kuba.kowalski@example.com", "ABC123456", "Jan Kuba", "Kowalski", "500678200", LocalDate.of(1985, 3, 12));
        when(patientMapper.toDto(any(Patient.class))).thenReturn(exceptedDto);

        UpdatePatientCommand command = new UpdatePatientCommand(new Email("jan.kuba.kowalski@example.com"), "Jan Kuba", "Kowalski", "500678200", LocalDate.of(1985, 3, 12));

        // when
        PatientDto result = patientService.update(email, command);

        // then
        assertAll(() -> assertEquals("Jan Kuba", result.firstName()), () -> assertEquals("jan.kuba.kowalski@example.com", result.email()), () -> assertEquals("500678200", result.phoneNumber()));
        verify(patientRepository, times(1)).update(eq(email), any(Patient.class));
    }

    @Test
    void delete_PatientExists_PatientDeleted() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        when(patientRepository.delete(email)).thenReturn(true);

        // when
        patientService.delete(email);

        // then
        verify(patientRepository, times(1)).delete(email);
    }

    @Test
    void delete_PatientNotFound_ExceptionThrows() {
        // given
        Email email = new Email("maciek.nowak@onet.pl");
        when(patientRepository.delete(email)).thenReturn(false);

        // when
        PatientNotFoundException exception = assertThrows(PatientNotFoundException.class, () -> patientService.delete(email));

        // then
        assertEquals("Patient not found: " + email, exception.getMessage());
        verify(patientRepository, times(1)).delete(email);
    }

    @Test
    void changePassword_ChangePatientPassword_MatchChangedPassword() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(new CreatePatientCommand(email, "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123"));
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        ChangePasswordPatientCommand command = new ChangePasswordPatientCommand("haslo123", "nowehaslo123");

        // when
        patientService.changePassword(email, command);
        Patient result = patientRepository.findByEmail(email).orElseThrow();

        // then
        verify(patientRepository, times(2)).findByEmail(email);
        assertTrue(passwordEncoder.matches("nowehaslo123", result.getPasswordHash()));
    }

    @Test
    void changePassword_ChangePatientPassword_ExceptionThrowsPatientCurrentPasswordIsTheSame() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(new CreatePatientCommand(email, "haslo123", "ABC123456", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123"));
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        ChangePasswordPatientCommand command = new ChangePasswordPatientCommand("haslo123", "haslo123");

        // when
        PatientCurrentPasswordIsTheSame exception = assertThrows(PatientCurrentPasswordIsTheSame.class, () -> patientService.changePassword(email, command));

        // then
        assertEquals("New password must differ from current one!", exception.getMessage());
        verify(patientRepository, times(1)).findByEmail(email);
    }
}
