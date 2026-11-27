package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.CreateVisitCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.SearchVisitCommand;
import com.arturoller.medicalclinic.dto.VisitDto;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VisitController.class)
class VisitControllerTest {
    private static final LocalDateTime START = LocalDateTime.of(2030, 1, 15, 9, 0);
    private static final LocalDateTime END = START.plusMinutes(15);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    VisitService visitService;

    // ---------- GET /visits ----------

    @Test
    void search_WithFilters_PassesQueryToService() throws Exception {
        // given
        SearchVisitCommand query = new SearchVisitCommand(1L, 2L,
                LocalDateTime.of(2030, 1, 15, 0, 0), LocalDateTime.of(2030, 1, 16, 0, 0));
        when(visitService.search(query, PageRequest.of(0, 10, Sort.by("startTime")))).thenReturn(new PageDto<>(
                List.of(new VisitDto(10L, 1L, 2L, null, START, END, true)),
                0, 10, 1L, 1, false, true));

        // when + then
        mockMvc.perform(get("/visits")
                        .param("doctorId", "1")
                        .param("institutionId", "2")
                        .param("from", "2030-01-15T00:00:00")
                        .param("to", "2030-01-16T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].available").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(visitService).search(query, PageRequest.of(0, 10, Sort.by("startTime")));
    }

    @Test
    void search_WithoutParams_PassesEmptyQueryAndDefaultPageableToService() throws Exception {
        // given
        when(visitService.search(any(), any())).thenReturn(new PageDto<>(List.of(), 0, 10, 0L, 0, false, true));

        // when + then
        mockMvc.perform(get("/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(visitService).search(new SearchVisitCommand(null, null, null, null),
                PageRequest.of(0, 10, Sort.by("startTime")));
    }

    @Test
    void search_PageAndSizeGiven_KeepsDefaultSortAndPassesPageableToService() throws Exception {
        // given
        when(visitService.search(any(), any())).thenReturn(new PageDto<>(
                List.of(new VisitDto(20L, 1L, 2L, null, START, END, true)),
                2, 5, 11L, 3, false, true));

        // when + then
        mockMvc.perform(get("/visits")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(20))
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(11));

        verify(visitService).search(new SearchVisitCommand(null, null, null, null),
                PageRequest.of(2, 5, Sort.by("startTime")));
    }

    @Test
    void search_SortGiven_ReplacesDefaultSort() throws Exception {
        // given
        when(visitService.search(any(), any())).thenReturn(new PageDto<>(List.of(), 0, 10, 0L, 0, false, true));

        // when + then
        mockMvc.perform(get("/visits").param("sort", "endTime,desc"))
                .andExpect(status().isOk());

        verify(visitService).search(new SearchVisitCommand(null, null, null, null),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "endTime")));
    }

    @Test
    void search_InvalidSortField_Returns400() throws Exception {
        // given
        when(visitService.search(new SearchVisitCommand(null, null, null, null), PageRequest.of(0, 10, Sort.by("xyz"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: xyz. Dozwolone pola: endTime, id, startTime"));

        // when + then
        mockMvc.perform(get("/visits").param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: xyz. Dozwolone pola: endTime, id, startTime"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void search_NonPositiveDoctorId_Returns400WithoutCallingService() throws Exception {
        // when + then
        mockMvc.perform(get("/visits").param("doctorId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("doctorId: musi być większe od 0"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(visitService);
    }

    @Test
    void search_InvalidFromFormat_Returns400WithoutCallingService() throws Exception {
        // when + then
        mockMvc.perform(get("/visits").param("from", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from: niepoprawny format wartości"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));

        verifyNoInteractions(visitService);
    }

    // ---------- GET /visits/{id} ----------

    @Test
    void getById_VisitExists_Returns200AndVisit() throws Exception {
        // given
        when(visitService.getById(10L)).thenReturn(new VisitDto(10L, 1L, 2L, 3L, START, END, false));

        // when + then
        mockMvc.perform(get("/visits/{visitId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.doctorId").value(1))
                .andExpect(jsonPath("$.institutionId").value(2))
                .andExpect(jsonPath("$.patientId").value(3))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void getById_VisitNotFound_Returns404() throws Exception {
        // given
        when(visitService.getById(10L)).thenThrow(new VisitNotFoundException("Nie znaleziono terminu: 10"));

        // when + then
        mockMvc.perform(get("/visits/{visitId}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono terminu: 10"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    // ---------- DELETE /visits/{id} ----------

    @Test
    void delete_FreeVisit_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/visits/{visitId}", 10L))
                .andExpect(status().isNoContent());

        verify(visitService).delete(10L);
    }

    @Test
    void delete_VisitNotFound_Returns404() throws Exception {
        // given
        doThrow(new VisitNotFoundException("Nie znaleziono terminu: 10")).when(visitService).delete(10L);

        // when + then
        mockMvc.perform(delete("/visits/{visitId}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono terminu: 10"));
    }

    @Test
    void delete_BookedVisit_Returns409() throws Exception {
        // given
        doThrow(new PatientAlreadyAssignedException("Termin 10 jest zarezerwowany przez pacjenta 3"))
                .when(visitService).delete(10L);

        // when + then
        mockMvc.perform(delete("/visits/{visitId}", 10L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Termin 10 jest zarezerwowany przez pacjenta 3"));
    }

    // ---------- POST /visits ----------

    @Test
    void create_ValidCommand_Returns201AndVisit() throws Exception {
        // given
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);
        when(visitService.create(command)).thenReturn(new VisitDto(10L, 1L, 2L, null, START, END, true));

        // when + then
        mockMvc.perform(post("/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.doctorId").value(1))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void create_InvalidTime_Returns400() throws Exception {
        // given
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);
        when(visitService.create(command)).thenThrow(new InvalidVisitTimeException("Termin musi zaczynać się w przyszłości"));

        // when + then
        mockMvc.perform(post("/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Termin musi zaczynać się w przyszłości"));
    }

    @Test
    void create_MissingDoctorId_Returns400WithoutCallingService() throws Exception {
        // given
        CreateVisitCommand command = new CreateVisitCommand(null, 2L, START, END);

        // when + then
        mockMvc.perform(post("/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("doctorId: jest wymagane"));

        verifyNoInteractions(visitService);
    }

    @Test
    void create_DoctorNotFound_Returns404() throws Exception {
        // given
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);
        when(visitService.create(command)).thenThrow(new DoctorNotFoundException("Nie znaleziono lekarza: 1"));

        // when + then
        mockMvc.perform(post("/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono lekarza: 1"));
    }

    @Test
    void create_OverlappingVisit_Returns409() throws Exception {
        // given
        CreateVisitCommand command = new CreateVisitCommand(1L, 2L, START, END);
        when(visitService.create(command))
                .thenThrow(new VisitOverlapException("Lekarz 1 ma już termin w przedziale 2030-01-15T09:00 – 2030-01-15T09:15"));

        // when + then
        mockMvc.perform(post("/visits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Lekarz 1 ma już termin w przedziale 2030-01-15T09:00 – 2030-01-15T09:15"));
    }

    // ---------- POST /visits/{id}/book/{patientId} ----------

    @Test
    void bookPatient_FreeVisit_Returns204() throws Exception {
        // when + then
        mockMvc.perform(post("/visits/{visitId}/book/{patientId}", 10L, 3L))
                .andExpect(status().isNoContent());

        verify(visitService).bookPatient(10L, 3L);
    }

    @Test
    void bookPatient_VisitNotFound_Returns404() throws Exception {
        // given
        doThrow(new VisitNotFoundException("Nie znaleziono terminu: 10")).when(visitService).bookPatient(10L, 3L);

        // when + then
        mockMvc.perform(post("/visits/{visitId}/book/{patientId}", 10L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono terminu: 10"));
    }

    @Test
    void bookPatient_VisitAlreadyBooked_Returns409() throws Exception {
        // given
        doThrow(new VisitAlreadyBookedException("Termin 10 jest już zarezerwowany")).when(visitService).bookPatient(10L, 3L);

        // when + then
        mockMvc.perform(post("/visits/{visitId}/book/{patientId}", 10L, 3L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Termin 10 jest już zarezerwowany"));
    }

    // ---------- DELETE /visits/{id}/cancel/{patientId} ----------

    @Test
    void cancelPatient_BookedVisit_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/visits/{visitId}/cancel/{patientId}", 10L, 3L))
                .andExpect(status().isNoContent());

        verify(visitService).cancelPatient(10L, 3L);
    }

    @Test
    void cancelPatient_VisitNotFound_Returns404() throws Exception {
        // given
        doThrow(new VisitNotFoundException("Nie znaleziono terminu: 10")).when(visitService).cancelPatient(10L, 3L);

        // when + then
        mockMvc.perform(delete("/visits/{visitId}/cancel/{patientId}", 10L, 3L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono terminu: 10"));
    }

    @Test
    void cancelPatient_NotBookedByPatient_Returns409() throws Exception {
        // given
        doThrow(new VisitNotBookedByPatientException("Termin 10 nie jest zarezerwowany przez pacjenta 3"))
                .when(visitService).cancelPatient(10L, 3L);

        // when + then
        mockMvc.perform(delete("/visits/{visitId}/cancel/{patientId}", 10L, 3L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Termin 10 nie jest zarezerwowany przez pacjenta 3"));
    }
}
