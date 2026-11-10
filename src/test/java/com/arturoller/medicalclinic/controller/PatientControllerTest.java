package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.EmailAlreadyTakenException;
import com.arturoller.medicalclinic.exception.PatientCurrentPasswordIsTheSame;
import com.arturoller.medicalclinic.exception.PatientNotFoundException;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class PatientControllerTest {
    @MockitoBean
    PatientService patientService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void getAll_PatientsExists_ReturnListOfPatients() throws Exception {
        // given
        List<Patient> patients = List.of(
                Patient.from(new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456",
                        "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123")),
                Patient.from(new CreatePatientCommand(new Email("anna.nowak@example.com"), "haslo123", "DEF234567",
                        "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24)), passwordEncoder.encode("haslo123"))
        );
        when(patientService.getAll()).thenReturn(patients);

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.get("/patients"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].firstName").value("Jan"))
                .andExpect(jsonPath("$[0].lastName").value("Kowalski"))
                .andExpect(jsonPath("$[0].phoneNumber").value("500100200"))
                .andExpect(jsonPath("$[0].birthday").value("1985-03-12"))
                .andExpect(jsonPath("$[1].firstName").value("Anna"))
                .andExpect(jsonPath("$[1].lastName").value("Nowak"));
    }

    @Test
    void getAll_NoPatients_ReturnEmptyList() throws Exception {
        // given
        List<Patient> patients = List.of();
        when(patientService.getAll()).thenReturn(patients);

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByEmail_PatientExists_ReturnPatient() throws Exception {
        // given
        Patient patient = Patient.from(new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456",
                "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder.encode("haslo123"));
        when(patientService.findByEmail(patient.getEmail())).thenReturn(patient);

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.get("/patients/" + patient.getEmail().email()));
    }

    @Test
    void findByEmail_PatientNotExists_Returns404() throws Exception {
        // given
        Email email = new Email("maciek.nowak@onet.pl");
        when(patientService.findByEmail(email)).thenThrow(new PatientNotFoundException("Patient not found: " + email, HttpStatus.NOT_FOUND));

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.get("/patients/{email}", "maciek.nowak@onet.pl"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_ValidPatient_Returns201AndPatientDto() throws Exception {
        // given
        CreatePatientCommand command = new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456",
        "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12));
        when(patientService.create(any(CreatePatientCommand.class))).thenReturn(new PatientDto("jan.kowalski@example.com", "ABC123456", "Jan", "Kowalski",
                "500100200", LocalDate.of(1985, 3, 12)));

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.post("/patients")
                        .content(objectMapper.writeValueAsString(command))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jan.kowalski@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.phoneNumber").value("500100200"));

        verify(patientService, times(1)).create(any(CreatePatientCommand.class));
    }

    @Test
    void create_MalformedJson_Returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/patients")
                        .content("{ to nie jest json")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(patientService);
    }

    @Test
    void update_PatientExists_ReturnsUpdatedPatient() throws Exception {
        // given
        Email email = new Email("jan.kowalski@example.com");
        UpdatePatientCommand command = new UpdatePatientCommand(
                new Email("jan.kuba.kowalski@example.com"), "Jan Kuba", "Kowalski",
                "500678200", LocalDate.of(1985, 3, 12));
        PatientDto expectedDto = new PatientDto(
                "jan.kuba.kowalski@example.com", "ABC123456", "Jan Kuba", "Kowalski",
                "500678200", LocalDate.of(1985, 3, 12));
        when(patientService.update(eq(email), any(UpdatePatientCommand.class))).thenReturn(expectedDto);

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.patch("/patients/{email}", "jan.kowalski@example.com")
                        .content(objectMapper.writeValueAsString(command))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jan.kuba.kowalski@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jan Kuba"))
                .andExpect(jsonPath("$.phoneNumber").value("500678200"));

        verify(patientService, times(1)).update(eq(email), any(UpdatePatientCommand.class));
    }

    @Test
    void update_NewEmailAlreadyTaken_Returns409() throws Exception {
        // given
        Email email = new Email("jan.kowalski@example.com");
        UpdatePatientCommand command = new UpdatePatientCommand(
                new Email("anna.nowak@example.com"), "Jan", "Kowalski",
                "500100200", LocalDate.of(1985, 3, 12));
        when(patientService.update(eq(email), any(UpdatePatientCommand.class)))
                .thenThrow(new EmailAlreadyTakenException(
                        "Email already exists: anna.nowak@example.com", HttpStatus.CONFLICT));

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.patch("/patients/{email}", "jan.kowalski@example.com")
                        .content(objectMapper.writeValueAsString(command))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void changePassword_NewPassword_Returns200() throws Exception {
        // given
        Email email = new Email("jan.kowalski@example.com");
        ChangePasswordPatientCommand command = new ChangePasswordPatientCommand( "haslo123", "nowehaslo123");

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.patch("/patients/{email}/password", "jan.kowalski@example.com")
                        .content(objectMapper.writeValueAsString(command))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(patientService, times(1)).changePassword(eq(email), eq(command));
    }

    @Test
    void changePassword_SamePassword_Returns400() throws Exception {
        // given
        Email email = new Email("jan.kowalski@example.com");
        ChangePasswordPatientCommand command = new ChangePasswordPatientCommand( "haslo123", "haslo123");
        doThrow(new PatientCurrentPasswordIsTheSame("New password must differ from current one!", HttpStatus.CONFLICT))
                .when(patientService).changePassword(eq(email), any(ChangePasswordPatientCommand.class));

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.patch("/patients/{email}/password", "jan.kowalski@example.com")
                        .content(objectMapper.writeValueAsString(command))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_PatientExists_Returns204() throws Exception {
        // given
        Email email = new Email("jan.kowalski@example.com");

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.delete("/patients/{email}", "jan.kowalski@example.com"))
                .andExpect(status().isNoContent());

        verify(patientService, times(1)).delete(email);
    }

    @Test
    void delete_PatientNotFound_Returns404() throws Exception {
        // given
        Email email = new Email("maciek.nowak@onet.pl");
        doThrow(new PatientNotFoundException("Patient not found: " + email, HttpStatus.NOT_FOUND)).when(patientService).delete(email);

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.delete("/patients/{email}", "maciek.nowak@onet.pl"))
                .andExpect(status().isNotFound());
    }

}
