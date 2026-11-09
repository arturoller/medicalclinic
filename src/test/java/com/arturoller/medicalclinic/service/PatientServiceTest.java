package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exceptions.PatientCurrentPasswordIsTheSame;
import com.arturoller.medicalclinic.exceptions.PatientNotFoundException;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.repisitory.PatientRepository;
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
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.patientService = new PatientService(patientRepository, patientMapper, passwordEncoder);
    }

    @Test
    void getAll_PatientsExsists_PatientsReturned() {
        // given
        List<Patient> patients = List.of(
                Patient.from(new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456",
                        "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder),
                Patient.from(new CreatePatientCommand(new Email("anna.nowak@example.com"), "haslo123", "DEF234567",
                        "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24)), passwordEncoder)
        );
        when(patientRepository.findAll()).thenReturn(patients);

        // when
        List<Patient> result = patientService.getAll();

        // then
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertEquals("Jan", result.get(0).getFirstName()),
                () -> assertEquals("Nowak", result.get(1).getLastName()),
                () -> assertInstanceOf(Email.class, result.get(0).getEmail()),
                () -> assertTrue(result.get(1).passwordMatches("haslo123", passwordEncoder))
        );
    }

    @Test
    void finByEmail_PatientExsists_PatientsReturned() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(
                new CreatePatientCommand(
                        email,
                        "haslo123",
                        "ABC123456",
                        "Jan",
                        "Kowalski",
                        "500100200",
                        LocalDate.of(1985, 3, 12)
                ),
                passwordEncoder);
        when(patientRepository.findByEmail(
                email
        )).thenReturn(Optional.of(patient));

        // when
        Patient result = patientRepository.findByEmail(email).orElseThrow();

        // then
        assertAll(
                () -> assertEquals("Jan", result.getFirstName()),
                () -> assertEquals("Kowalski", result.getLastName()),
                () -> assertInstanceOf(Email.class, result.getEmail()),
                () -> assertTrue(result.passwordMatches("haslo123", passwordEncoder))
        );
        verify(patientRepository, times(1)).findByEmail(eq(email));
    }

    @Test
    void findByEmail_PatientNotExsists_ThrowsException() {
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(
                new CreatePatientCommand(
                        email,
                        "haslo123",
                        "ABC123456",
                        "Jan",
                        "Kowalski",
                        "500100200",
                        LocalDate.of(1985, 3, 12)
                ),
                passwordEncoder);
        when(patientRepository.findByEmail(
                email
        )).thenReturn(Optional.of(patient));
    }

    @Test
    void create_CreatePatient_ReturnPatientDto() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        CreatePatientCommand command = new CreatePatientCommand(
                email,
                "haslo123",
                "ABC123456",
                "Jan",
                "Kowalski",
                "500100200",
                LocalDate.of(1985, 3, 12)
        );
        when(patientRepository.exists(email)).thenReturn(false);

        Patient patient = Patient.from(command, passwordEncoder);
        when(patientMapper.toModel(command)).thenReturn(patient);
        when(patientRepository.create(any(Patient.class))).thenReturn(patient);

        PatientDto exceptedDto = new PatientDto(
                "jan.kowalski@example.com",
                "ABC123456",
                "Jan",
                "Kowalski",
                "500100200",
                LocalDate.of(1985, 3, 12)
        );
        when(patientMapper.toDto(any(Patient.class))).thenReturn(exceptedDto);

        // when
        PatientDto result = patientService.create(command);

        // then
        assertAll(
                () -> assertEquals("Jan", result.firstName()),
                () -> assertEquals("jan.kowalski@example.com", result.email()),
                () -> assertEquals("500100200", result.phoneNumber())
        );
        verify(patientRepository, times(1)).create(any(Patient.class));
    }

    @Test
    void update_UpdatePatient_ReturnPatientDto() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(
                new CreatePatientCommand(
                        email,
                        "haslo123",
                        "ABC123456",
                        "Jan",
                        "Kowalski",
                        "500100200",
                        LocalDate.of(1985, 3, 12)
                ),
                passwordEncoder);
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        PatientDto exceptedDto = new PatientDto(
                "jan.kuba.kowalski@example.com",
                "ABC123456",
                "Jan Kuba",
                "Kowalski",
                "500678200",
                LocalDate.of(1985, 3, 12)
        );
        when(patientMapper.toDto(any(Patient.class))).thenReturn(exceptedDto);

        UpdatePatientCommand command = new UpdatePatientCommand(
                new Email("jan.kuba.kowalski@example.com"),
                "Jan Kuba",
                "Kowalski",
                "500678200",
                LocalDate.of(1985, 3, 12)
        );

        // when
        PatientDto result = patientService.update(email, command);

        // then
        assertAll(
                () -> assertEquals("Jan Kuba", result.firstName()),
                () -> assertEquals("jan.kuba.kowalski@example.com", result.email()),
                () -> assertEquals("500678200", result.phoneNumber())
        );
        verify(patientRepository, times(1)).update(eq(email), any(Patient.class));
    }

    @Test
    void delete_PatientExsist_PatientDeleted() {
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
        Patient patient = Patient.from(
                new CreatePatientCommand(
                        email,
                        "haslo123",
                        "ABC123456",
                        "Jan",
                        "Kowalski",
                        "500100200",
                        LocalDate.of(1985, 3, 12)
                ),
                passwordEncoder);
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        ChangePasswordPatientCommand command = new ChangePasswordPatientCommand(
                "nowehaslo123"
        );

        // when
        patientService.changePassword(email, command);
        Patient result = patientRepository.findByEmail(email).orElseThrow();

        // then
        verify(patientRepository, times(2)).findByEmail(email);
        assertTrue(patient.passwordMatches("nowehaslo123", passwordEncoder));
    }

    @Test
    void changePassword_ChangePatientPassword_ExceptionThrowsPatientCurrentPasswordIsTheSame() {
        // given
        Email email = new Email("jan.kowalski@example.com");
        Patient patient = Patient.from(
                new CreatePatientCommand(
                        email,
                        "haslo123",
                        "ABC123456",
                        "Jan",
                        "Kowalski",
                        "500100200",
                        LocalDate.of(1985, 3, 12)
                ),
                passwordEncoder);
        when(patientRepository.findByEmail(email)).thenReturn(Optional.of(patient));

        ChangePasswordPatientCommand command = new ChangePasswordPatientCommand(
                "haslo123"
        );

        // when
        PatientCurrentPasswordIsTheSame exception = assertThrows(PatientCurrentPasswordIsTheSame.class, () -> patientService.changePassword(email, command));

        // then
        assertEquals("Current password is the same! Can't change password!", exception.getMessage());
        verify(patientRepository, times(1)).findByEmail(email);
    }
}
