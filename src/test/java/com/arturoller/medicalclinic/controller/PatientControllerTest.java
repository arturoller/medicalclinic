package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
    ObjectMapper objectMapper;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void getAll_PatientsExsist_ReturnListOfPatients() throws Exception {
        // given
        List<Patient> patients = List.of(
                Patient.from(new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456",
                        "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder),
                Patient.from(new CreatePatientCommand(new Email("anna.nowak@example.com"), "haslo123", "DEF234567",
                        "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24)), passwordEncoder)
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
    void findByEmail_PatientExsist_ReturnPatient() throws Exception {
        // given
        Patient patient = Patient.from(new CreatePatientCommand(new Email("jan.kowalski@example.com"), "haslo123", "ABC123456",
                "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)), passwordEncoder);
        when(patientService.findByEmail(patient.getEmail())).thenReturn(patient);

        // when + then
        mockMvc.perform(MockMvcRequestBuilders.get("/patients/" + patient.getEmail().email()));
    }
}
