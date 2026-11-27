package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.CreateInstitutionCommand;
import com.arturoller.medicalclinic.dto.InstitutionDto;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.UpdateInstitutionCommand;
import com.arturoller.medicalclinic.exception.InstitutionAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.InstitutionNameAlreadyTakenException;
import com.arturoller.medicalclinic.exception.InstitutionNotFoundException;
import com.arturoller.medicalclinic.exception.InvalidSortFieldException;
import com.arturoller.medicalclinic.service.InstitutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InstitutionController.class)
class InstitutionControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    InstitutionService institutionService;

    @Test
    void list_InstitutionsExist_Returns200AndPage() throws Exception {
        // given
        when(institutionService.list(PageRequest.of(0, 10))).thenReturn(new PageDto<>(
                List.of(new InstitutionDto(1L, "Przychodnia Zdrowie", "Warszawa", "00-950", "Marszałkowska", "10"),
                        new InstitutionDto(2L, "Centrum Medyczne Vita", "Kraków", "31-010", "Długa", "5")),
                0, 10, 2L, 1, false, true));

        // when + then
        mockMvc.perform(get("/institutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Przychodnia Zdrowie"))
                .andExpect(jsonPath("$.content[1].city").value("Kraków"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_PageSizeAndSortGiven_PassesPageableToService() throws Exception {
        // given
        when(institutionService.list(any())).thenReturn(new PageDto<>(
                List.of(new InstitutionDto(2L, "Centrum Medyczne Vita", "Kraków", "31-010", "Długa", "5")),
                0, 1, 2L, 2, true, false));

        // when + then
        mockMvc.perform(get("/institutions")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "city,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.pageSize").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(institutionService).list(PageRequest.of(0, 1, Sort.by("city")));
    }

    @Test
    void list_InvalidSortField_Returns400() throws Exception {
        // given
        when(institutionService.list(PageRequest.of(0, 10, Sort.by("xyz"))))
                .thenThrow(new InvalidSortFieldException("Niedozwolone pole sortowania: xyz. Dozwolone pola: city, id, name"));

        // when + then
        mockMvc.perform(get("/institutions").param("sort", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Niedozwolone pole sortowania: xyz. Dozwolone pola: city, id, name"))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void getById_InstitutionExists_Returns200AndInstitution() throws Exception {
        // given
        when(institutionService.getById(1L))
                .thenReturn(new InstitutionDto(1L, "Przychodnia Zdrowie", "Warszawa", "00-950", "Marszałkowska", "10"));

        // when + then
        mockMvc.perform(get("/institutions/{institutionId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Przychodnia Zdrowie"))
                .andExpect(jsonPath("$.postCode").value("00-950"))
                .andExpect(jsonPath("$.buildingNumber").value("10"));
    }

    @Test
    void getById_InstitutionNotFound_Returns404() throws Exception {
        // given
        when(institutionService.getById(1L))
                .thenThrow(new InstitutionNotFoundException("Nie znaleziono placówki: 1"));

        // when + then
        mockMvc.perform(get("/institutions/{institutionId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono placówki: 1"))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void update_InstitutionExists_Returns200AndUpdatedInstitution() throws Exception {
        // given
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Przychodnia Zdrowie Plus", "Warszawa", "00-950", "Marszałkowska", "12");
        when(institutionService.update(1L, command))
                .thenReturn(new InstitutionDto(1L, "Przychodnia Zdrowie Plus", "Warszawa", "00-950", "Marszałkowska", "12"));

        // when + then
        mockMvc.perform(put("/institutions/{institutionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Przychodnia Zdrowie Plus"))
                .andExpect(jsonPath("$.buildingNumber").value("12"));
    }

    @Test
    void update_InstitutionNotFound_Returns404() throws Exception {
        // given
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Przychodnia Zdrowie Plus", "Warszawa", "00-950", "Marszałkowska", "12");
        when(institutionService.update(1L, command))
                .thenThrow(new InstitutionNotFoundException("Nie znaleziono placówki: 1"));

        // when + then
        mockMvc.perform(put("/institutions/{institutionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono placówki: 1"));
    }

    @Test
    void update_NameTaken_Returns409() throws Exception {
        // given
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Centrum Medyczne Vita", "Warszawa", "00-950", "Marszałkowska", "10");
        when(institutionService.update(1L, command))
                .thenThrow(new InstitutionNameAlreadyTakenException("Placówka o nazwie Centrum Medyczne Vita już istnieje"));

        // when + then
        mockMvc.perform(put("/institutions/{institutionId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Placówka o nazwie Centrum Medyczne Vita już istnieje"));
    }

    @Test
    void delete_InstitutionWithoutDoctors_Returns204() throws Exception {
        // when + then
        mockMvc.perform(delete("/institutions/{institutionId}", 1L))
                .andExpect(status().isNoContent());

        verify(institutionService).delete(1L);
    }

    @Test
    void delete_InstitutionNotFound_Returns404() throws Exception {
        // given
        doThrow(new InstitutionNotFoundException("Nie znaleziono placówki: 1"))
                .when(institutionService).delete(1L);

        // when + then
        mockMvc.perform(delete("/institutions/{institutionId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Nie znaleziono placówki: 1"));
    }

    @Test
    void delete_InstitutionHasDoctors_Returns409() throws Exception {
        // given
        doThrow(new InstitutionAlreadyAssignedException("Placówka 1 ma przypisanych lekarzy"))
                .when(institutionService).delete(1L);

        // when + then
        mockMvc.perform(delete("/institutions/{institutionId}", 1L))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Placówka 1 ma przypisanych lekarzy"));
    }

    @Test
    void create_NameFree_Returns201AndInstitution() throws Exception {
        // given
        CreateInstitutionCommand command = new CreateInstitutionCommand("Szpital Miejski", "Gdańsk", "80-210", "Grunwaldzka", "100");
        when(institutionService.save(command))
                .thenReturn(new InstitutionDto(3L, "Szpital Miejski", "Gdańsk", "80-210", "Grunwaldzka", "100"));

        // when + then
        mockMvc.perform(post("/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Szpital Miejski"));
    }

    @Test
    void create_BlankName_Returns400WithoutCallingService() throws Exception {
        // given
        CreateInstitutionCommand command = new CreateInstitutionCommand(" ", "Gdańsk", "80-210", "Grunwaldzka", "100");

        // when + then
        mockMvc.perform(post("/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("name: nie może być puste"));

        verifyNoInteractions(institutionService);
    }

    @Test
    void create_NameTaken_Returns409() throws Exception {
        // given
        CreateInstitutionCommand command = new CreateInstitutionCommand("Szpital Miejski", "Gdańsk", "80-210", "Grunwaldzka", "100");
        when(institutionService.save(command))
                .thenThrow(new InstitutionNameAlreadyTakenException("Placówka o nazwie Szpital Miejski już istnieje"));

        // when + then
        mockMvc.perform(post("/institutions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Placówka o nazwie Szpital Miejski już istnieje"));
    }
}
