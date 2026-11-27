package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    DoctorService doctorService;

    // ---------- GET /doctors, GET /doctors/{id} ----------

    @Test
    void list_DoctorsExist_Returns200AndPage() throws Exception {
        // given
        when(doctorService.list(PageRequest.of(0, 10))).thenReturn(new PageDto<>(
                List.of(new DoctorDto(1L, "Ewa", "Zielińska", "600100200", 11L),
                        new DoctorDto(2L, "Marek", "Lewandowski", "601200300", 12L)),
                0, 10, 2L, 1, false, true));

        // when + then
        mockMvc.perform(get("/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].lastName").value("Zielińska"))
                .andExpect(jsonPath("$.content[1].userId").value(12))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_PageSizeAndSortGiven_PassesPageableToService() throws Exception {
        // given
        when(doctorService.list(any())).thenReturn(new PageDto<>(
                List.of(new DoctorDto(2L, "Marek", "Lewandowski", "601200300", 12L)),
                1, 2, 3L, 2, false, true));

        // when + then
        mockMvc.perform(get("/doctors")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "lastName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        verify(doctorService).list(PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "lastName")));
    }

    @Test
    void list_InvalidSortField_Returns400() throws Exception {
        // given
        when(doctorService.list(PageRequest.of(0, 10, Sort.by("xyz"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: xyz. Dozwolone pola: firstName, id, lastName"));

        // when + then
        mockMvc.perform(get("/doctors").param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: xyz. Dozwolone pola: firstName, id, lastName"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void getById_DoctorExists_Returns200AndDoctor() throws Exception {
        // given
        when(doctorService.getById(1L)).thenReturn(new DoctorDto(1L, "Ewa", "Zielińska", "600100200", 11L));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Ewa"))
                .andExpect(jsonPath("$.phoneNumber").value("600100200"))
                .andExpect(jsonPath("$.userId").value(11));
    }

    @Test
    void getById_DoctorNotFound_Returns404() throws Exception {
        // given
        when(doctorService.getById(1L)).thenThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    // ---------- PUT /doctors/{id} ----------

    @Test
    void update_DoctorExists_Returns200AndUpdatedDoctor() throws Exception {
        // given
        UpdateDoctorCommand command = new UpdateDoctorCommand("Ewa Maria", "Zielińska", "600999999");
        when(doctorService.update(1L, command)).thenReturn(new DoctorDto(1L, "Ewa Maria", "Zielińska", "600999999", 11L));

        // when + then
        mockMvc.perform(put("/doctors/{doctorId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ewa Maria"))
                .andExpect(jsonPath("$.phoneNumber").value("600999999"));
    }

    @Test
    void update_DoctorNotFound_Returns404() throws Exception {
        // given
        UpdateDoctorCommand command = new UpdateDoctorCommand("Ewa Maria", "Zielińska", "600999999");
        when(doctorService.update(1L, command)).thenThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"));

        // when + then
        mockMvc.perform(put("/doctors/{doctorId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    // ---------- DELETE /doctors/{id} ----------

    @Test
    void delete_DoctorWithoutAssignments_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}", 1L))
                .andExpect(status().isNoContent());

        verify(doctorService).delete(1L);
    }

    @Test
    void delete_DoctorNotFound_Returns404() throws Exception {
        // given
        doThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1")).when(doctorService).delete(1L);

        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    @Test
    void delete_DoctorHasAssignments_Returns409() throws Exception {
        // given
        doThrow(new DoctorAlreadyAssignedException("Lekarz 1 ma przypisanych pacjentów"))
                .when(doctorService).delete(1L);

        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Lekarz 1 ma przypisanych pacjentów"))
                .andExpect(jsonPath("$.status").value("CONFLICT"));
    }

    // ---------- POST /doctors ----------

    @Test
    void create_ValidCommand_Returns201AndDoctor() throws Exception {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(5L, "Marek", "Lewandowski", "601200300");
        when(doctorService.save(command)).thenReturn(new DoctorDto(2L, "Marek", "Lewandowski", "601200300", 5L));

        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.lastName").value("Lewandowski"))
                .andExpect(jsonPath("$.userId").value(5));
    }

    @Test
    void create_MissingUserId_Returns400WithoutCallingService() throws Exception {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(null, "Marek", "Lewandowski", "601200300");

        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId: jest wymagane"));

        verifyNoInteractions(doctorService);
    }

    @Test
    void create_NonPositiveUserId_Returns400WithoutCallingService() throws Exception {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(0L, "Marek", "Lewandowski", "601200300");

        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("userId: musi być większe od 0"));

        verifyNoInteractions(doctorService);
    }

    @Test
    void create_UserNotFound_Returns404() throws Exception {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(5L, "Marek", "Lewandowski", "601200300");
        when(doctorService.save(command)).thenThrow(new UserNotFoundException("Nie znaleziono użytkownika: 5"));

        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono użytkownika: 5"));
    }

    @Test
    void create_UserAlreadyDoctor_Returns409() throws Exception {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(5L, "Marek", "Lewandowski", "601200300");
        when(doctorService.save(command))
                .thenThrow(new UserAlreadyAssignedException("Użytkownik 5 jest już przypisany do innego lekarza"));

        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Użytkownik 5 jest już przypisany do innego lekarza"));
    }

    // ---------- /doctors/{id}/patients ----------

    @Test
    void getPatients_DoctorExists_Returns200AndPage() throws Exception {
        // given
        when(doctorService.getPatients(1L, PageRequest.of(0, 10))).thenReturn(new PageDto<>(
                List.of(new PatientDto(2L, "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12), 12L)),
                0, 10, 1L, 1, false, true));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/patients", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].lastName").value("Kowalski"))
                .andExpect(jsonPath("$.content[0].birthday").value("1985-03-12"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getPatients_PageSizeAndSortGiven_PassesPageableToService() throws Exception {
        // given
        when(doctorService.getPatients(eq(1L), any(Pageable.class))).thenReturn(new PageDto<>(
                List.of(new PatientDto(4L, "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24), 14L)),
                1, 5, 6L, 2, false, true));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/patients", 1L)
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "lastName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(6));

        verify(doctorService).getPatients(1L, PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "lastName")));
    }

    @Test
    void getPatients_InvalidSortField_Returns400() throws Exception {
        // given
        when(doctorService.getPatients(1L, PageRequest.of(0, 10, Sort.by("xyz"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: xyz. Dozwolone pola: birthday, firstName, id, lastName"));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/patients", 1L).param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: xyz. Dozwolone pola: birthday, firstName, id, lastName"));
    }

    @Test
    void getPatients_DoctorNotFound_Returns404() throws Exception {
        // given
        when(doctorService.getPatients(eq(1L), any(Pageable.class)))
                .thenThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/patients", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    @Test
    void assignPatient_NotAssignedYet_Returns204() throws Exception {
        // when + then
        mockMvc.perform(post("/doctors/{doctorId}/patients/{patientId}", 1L, 2L))
                .andExpect(status().isNoContent());

        verify(doctorService).assignPatient(1L, 2L);
    }

    @Test
    void assignPatient_PatientNotFound_Returns404() throws Exception {
        // given
        doThrow(new PatientNotFoundException("Nie znaleziono pacjenta: 2"))
                .when(doctorService).assignPatient(1L, 2L);

        // when + then
        mockMvc.perform(post("/doctors/{doctorId}/patients/{patientId}", 1L, 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono pacjenta: 2"));
    }

    @Test
    void assignPatient_AlreadyAssigned_Returns409() throws Exception {
        // given
        doThrow(new PatientAlreadyAssignedException("Pacjent 2 jest już przypisany do lekarza 1"))
                .when(doctorService).assignPatient(1L, 2L);

        // when + then
        mockMvc.perform(post("/doctors/{doctorId}/patients/{patientId}", 1L, 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Pacjent 2 jest już przypisany do lekarza 1"));
    }

    @Test
    void unassignPatient_Assigned_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}/patients/{patientId}", 1L, 2L))
                .andExpect(status().isNoContent());

        verify(doctorService).unassignPatient(1L, 2L);
    }

    @Test
    void unassignPatient_DoctorNotFound_Returns404() throws Exception {
        // given
        doThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"))
                .when(doctorService).unassignPatient(1L, 2L);

        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}/patients/{patientId}", 1L, 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    @Test
    void unassignPatient_NotAssigned_Returns409() throws Exception {
        // given
        doThrow(new PatientNotAssignedException("Pacjent 2 nie jest przypisany do lekarza 1"))
                .when(doctorService).unassignPatient(1L, 2L);

        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}/patients/{patientId}", 1L, 2L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Pacjent 2 nie jest przypisany do lekarza 1"));
    }

    // ---------- /doctors/{id}/institutions ----------

    @Test
    void getInstitutions_DoctorExists_Returns200AndPage() throws Exception {
        // given
        when(doctorService.getInstitutions(1L, PageRequest.of(0, 10))).thenReturn(new PageDto<>(
                List.of(new InstitutionDto(3L, "Przychodnia Zdrowie", "Warszawa", "00-950", "Marszałkowska", "10")),
                0, 10, 1L, 1, false, true));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/institutions", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Przychodnia Zdrowie"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getInstitutions_PageSizeAndSortGiven_PassesPageableToService() throws Exception {
        // given
        when(doctorService.getInstitutions(eq(1L), any(Pageable.class))).thenReturn(new PageDto<>(
                List.of(new InstitutionDto(5L, "Centrum Medyczne Vita", "Kraków", "31-010", "Długa", "5")),
                2, 1, 3L, 3, false, true));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/institutions", 1L)
                        .param("page", "2")
                        .param("size", "1")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));

        verify(doctorService).getInstitutions(1L, PageRequest.of(2, 1, Sort.by("name")));
    }

    @Test
    void getInstitutions_InvalidSortField_Returns400() throws Exception {
        // given
        when(doctorService.getInstitutions(1L, PageRequest.of(0, 10, Sort.by("xyz"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: xyz. Dozwolone pola: city, id, name"));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/institutions", 1L).param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: xyz. Dozwolone pola: city, id, name"));
    }

    @Test
    void getInstitutions_DoctorNotFound_Returns404() throws Exception {
        // given
        when(doctorService.getInstitutions(eq(1L), any(Pageable.class)))
                .thenThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"));

        // when + then
        mockMvc.perform(get("/doctors/{doctorId}/institutions", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    @Test
    void assignInstitution_NotAssignedYet_Returns204() throws Exception {
        // when + then
        mockMvc.perform(post("/doctors/{doctorId}/institutions/{institutionId}", 1L, 3L))
                .andExpect(status().isNoContent());

        verify(doctorService).assignInstitution(1L, 3L);
    }

    @Test
    void assignInstitution_InstitutionNotFound_Returns404() throws Exception {
        // given
        doThrow(new InstitutionNotFoundException("Nie znaleziono placówki: 3"))
                .when(doctorService).assignInstitution(1L, 3L);

        // when + then
        mockMvc.perform(post("/doctors/{doctorId}/institutions/{institutionId}", 1L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono placówki: 3"));
    }

    @Test
    void assignInstitution_AlreadyAssigned_Returns409() throws Exception {
        // given
        doThrow(new InstitutionAlreadyAssignedException("Placówka 3 jest już przypisana do lekarza 1"))
                .when(doctorService).assignInstitution(1L, 3L);

        // when + then
        mockMvc.perform(post("/doctors/{doctorId}/institutions/{institutionId}", 1L, 3L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Placówka 3 jest już przypisana do lekarza 1"));
    }

    @Test
    void unassignInstitution_Assigned_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}/institutions/{institutionId}", 1L, 3L))
                .andExpect(status().isNoContent());

        verify(doctorService).unassignInstitution(1L, 3L);
    }

    @Test
    void unassignInstitution_DoctorNotFound_Returns404() throws Exception {
        // given
        doThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"))
                .when(doctorService).unassignInstitution(1L, 3L);

        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}/institutions/{institutionId}", 1L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    @Test
    void unassignInstitution_NotAssigned_Returns409() throws Exception {
        // given
        doThrow(new InstitutionNotAssignedException("Placówka 3 nie jest przypisana do lekarza 1"))
                .when(doctorService).unassignInstitution(1L, 3L);

        // when + then
        mockMvc.perform(delete("/doctors/{doctorId}/institutions/{institutionId}", 1L, 3L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Placówka 3 nie jest przypisana do lekarza 1"));
    }

    // ---------- błędy frameworka -> polskie komunikaty (MedicalClinicExceptionHandler) ----------

    @Test
    void create_MalformedJson_Returns400WithPolishMessage() throws Exception {
        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ to nie jest json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Nie można odczytać treści żądania – sprawdź poprawność JSON-a i formaty pól"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(doctorService);
    }

    @Test
    void getById_NonNumericId_Returns400WithPolishMessage() throws Exception {
        // when + then
        mockMvc.perform(get("/doctors/{doctorId}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niepoprawna wartość parametru doctorId: abc"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(doctorService);
    }

    @Test
    void unsupportedMethod_PatchOnDoctors_Returns405WithPolishMessage() throws Exception {
        // when + then
        mockMvc.perform(patch("/doctors"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value(startsWith("Metoda PATCH nie jest obsługiwana")))
                .andExpect(jsonPath("$.status").value("METHOD_NOT_ALLOWED"));

        verifyNoInteractions(doctorService);
    }

    @Test
    void create_BlankFirstName_Returns400WithPolishValidationMessage() throws Exception {
        // given
        CreateDoctorCommand command = new CreateDoctorCommand(5L, " ", "Lewandowski", "601200300");

        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("firstName: nie może być puste")))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(doctorService);
    }

    @Test
    void create_EmptyBody_Returns400ListingEveryInvalidField() throws Exception {
        // when + then
        mockMvc.perform(post("/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("userId: jest wymagane"),
                        containsString("firstName: nie może być puste"),
                        containsString("lastName: nie może być puste"),
                        containsString("phoneNumber: nie może być puste"))));

        verifyNoInteractions(doctorService);
    }
}
