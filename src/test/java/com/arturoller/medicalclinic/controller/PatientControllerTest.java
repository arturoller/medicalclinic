package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.InvalidSortFieldException;
import com.arturoller.medicalclinic.exception.PatientAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.PatientNotFoundException;
import com.arturoller.medicalclinic.exception.UserAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.UserNotFoundException;
import com.arturoller.medicalclinic.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
class PatientControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    PatientService patientService;

    @Test
    void list_PatientsExist_Returns200AndPage() throws Exception {
        // given
        when(patientService.list(PageRequest.of(0, 10))).thenReturn(new PageDto<>(
                List.of(new PatientDto(1L, "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12), 11L),
                        new PatientDto(2L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24), 12L)),
                0, 10, 2L, 1, false, true));

        // when + then
        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].firstName").value("Jan"))
                .andExpect(jsonPath("$.content[0].birthday").value("1985-03-12"))
                .andExpect(jsonPath("$.content[1].lastName").value("Nowak"))
                .andExpect(jsonPath("$.content[1].userId").value(12))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_PageSizeAndSortGiven_PassesPageableToService() throws Exception {
        // given
        when(patientService.list(any())).thenReturn(new PageDto<>(
                List.of(new PatientDto(2L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24), 12L)),
                1, 1, 2L, 2, false, true));

        // when + then
        mockMvc.perform(get("/patients")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "birthday,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(patientService).list(PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "birthday")));
    }

    @Test
    void list_InvalidSortField_Returns400() throws Exception {
        // given
        when(patientService.list(PageRequest.of(0, 10, Sort.by("xyz"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: xyz. Dozwolone pola: birthday, firstName, id, lastName"));

        // when + then
        mockMvc.perform(get("/patients").param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: xyz. Dozwolone pola: birthday, firstName, id, lastName"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void getById_PatientExists_Returns200AndPatient() throws Exception {
        // given
        when(patientService.getById(1L))
                .thenReturn(new PatientDto(1L, "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12), 11L));

        // when + then
        mockMvc.perform(get("/patients/{patientId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.phoneNumber").value("500100200"))
                .andExpect(jsonPath("$.userId").value(11));
    }

    @Test
    void getById_PatientNotFound_Returns404() throws Exception {
        // given
        when(patientService.getById(1L)).thenThrow(new PatientNotFoundException("Nie znaleziono pacjenta: 1"));

        // when + then
        mockMvc.perform(get("/patients/{patientId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono pacjenta: 1"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void update_PatientExists_Returns200AndUpdatedPatient() throws Exception {
        // given
        UpdatePatientCommand command = new UpdatePatientCommand("Jan Kuba", "Kowalski", "500678200", LocalDate.of(1985, 3, 12));
        when(patientService.update(1L, command))
                .thenReturn(new PatientDto(1L, "Jan Kuba", "Kowalski", "500678200", LocalDate.of(1985, 3, 12), 11L));

        // when + then
        mockMvc.perform(put("/patients/{patientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jan Kuba"))
                .andExpect(jsonPath("$.phoneNumber").value("500678200"));
    }

    @Test
    void update_PatientNotFound_Returns404() throws Exception {
        // given
        UpdatePatientCommand command = new UpdatePatientCommand("Jan Kuba", "Kowalski", "500678200", LocalDate.of(1985, 3, 12));
        when(patientService.update(1L, command)).thenThrow(new PatientNotFoundException("Nie znaleziono pacjenta: 1"));

        // when + then
        mockMvc.perform(put("/patients/{patientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono pacjenta: 1"));
    }

    @Test
    void update_MissingBirthday_Returns400WithoutCallingService() throws Exception {
        // given
        UpdatePatientCommand command = new UpdatePatientCommand("Jan Kuba", "Kowalski", "500678200", null);

        // when + then
        mockMvc.perform(put("/patients/{patientId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("birthday: jest wymagane"));

        verifyNoInteractions(patientService);
    }

    @Test
    void delete_PatientWithoutDoctorsAndVisits_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/patients/{patientId}", 1L))
                .andExpect(status().isNoContent());

        verify(patientService).delete(1L);
    }

    @Test
    void delete_PatientNotFound_Returns404() throws Exception {
        // given
        doThrow(new PatientNotFoundException("Nie znaleziono pacjenta: 1")).when(patientService).delete(1L);

        // when + then
        mockMvc.perform(delete("/patients/{patientId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono pacjenta: 1"));
    }

    @Test
    void delete_PatientAssigned_Returns409() throws Exception {
        // given
        doThrow(new PatientAlreadyAssignedException("Pacjent 1 jest przypisany do lekarza")).when(patientService).delete(1L);

        // when + then
        mockMvc.perform(delete("/patients/{patientId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Pacjent 1 jest przypisany do lekarza"))
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    @Test
    void create_ValidCommand_Returns201AndPatient() throws Exception {
        // given
        CreatePatientCommand command = new CreatePatientCommand(5L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        when(patientService.create(command))
                .thenReturn(new PatientDto(1L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24), 5L));

        // when + then
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.birthday").value("1992-07-24"))
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void create_MissingUserId_Returns400WithoutCallingService() throws Exception {
        // given
        CreatePatientCommand command = new CreatePatientCommand(null, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));

        // when + then
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId: jest wymagane"));

        verifyNoInteractions(patientService);
    }

    @Test
    void create_UserNotFound_Returns404() throws Exception {
        // given
        CreatePatientCommand command = new CreatePatientCommand(5L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        when(patientService.create(command)).thenThrow(new UserNotFoundException("Nie znaleziono użytkownika: 5"));

        // when + then
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono użytkownika: 5"));
    }

    @Test
    void create_UserAlreadyHasPatient_Returns409() throws Exception {
        // given
        CreatePatientCommand command = new CreatePatientCommand(5L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        when(patientService.create(command))
                .thenThrow(new UserAlreadyAssignedException("Użytkownik 5 jest już przypisany do innego pacjenta"));

        // when + then
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Użytkownik 5 jest już przypisany do innego pacjenta"));
    }

    @Test
    void create_MalformedJson_Returns400() throws Exception {
        // when + then
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ to nie jest json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Nie można odczytać treści żądania – sprawdź poprawność JSON-a i formaty pól"));

        verifyNoInteractions(patientService);
    }

    @Test
    void create_InvalidBirthdayFormat_Returns400WithoutCallingService() throws Exception {
        // given
        String body = """
                {"userId": 5, "firstName": "Anna", "lastName": "Nowak", "phoneNumber": "501200300", "birthday": "24.07.1992"}
                """;

        // when + then
        mockMvc.perform(post("/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Nie można odczytać treści żądania – sprawdź poprawność JSON-a i formaty pól"));

        verifyNoInteractions(patientService);
    }
}
