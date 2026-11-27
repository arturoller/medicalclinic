package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.CreateDoctorCommand;
import com.arturoller.medicalclinic.dto.CreateInstitutionCommand;
import com.arturoller.medicalclinic.dto.InstitutionDto;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.UpdateInstitutionCommand;
import com.arturoller.medicalclinic.exception.InstitutionAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.InstitutionNameAlreadyTakenException;
import com.arturoller.medicalclinic.exception.InstitutionNotFoundException;
import com.arturoller.medicalclinic.exception.InvalidSortFieldException;
import com.arturoller.medicalclinic.mapper.InstitutionMapper;
import com.arturoller.medicalclinic.model.Doctor;
import com.arturoller.medicalclinic.model.Institution;
import com.arturoller.medicalclinic.repository.InstitutionRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InstitutionServiceTest {
    private InstitutionRepository institutionRepository;
    private VisitRepository visitRepository;
    private InstitutionService institutionService;

    @BeforeEach
    void setup() {
        institutionRepository = mock(InstitutionRepository.class);
        visitRepository = mock(VisitRepository.class);
        institutionService = new InstitutionService(institutionRepository, visitRepository,
                Mappers.getMapper(InstitutionMapper.class), new SortValidator());
    }

    @Test
    void list_InstitutionsExist_ReturnsPageOfInstitutionDtos() {
        // given
        Pageable pageable = PageRequest.of(1, 2);
        when(institutionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                institution(3L, "Przychodnia Zdrowie"),
                institution(4L, "Centrum Medyczne Vita")), pageable, 5));

        // when
        PageDto<InstitutionDto> result = institutionService.list(pageable);

        // then
        assertAll(
                () -> assertEquals(2, result.content().size()),
                () -> assertEquals(3L, result.content().get(0).id()),
                () -> assertEquals("Przychodnia Zdrowie", result.content().get(0).name()),
                () -> assertEquals("Centrum Medyczne Vita", result.content().get(1).name()),
                () -> assertEquals(1, result.pageNumber()),
                () -> assertEquals(2, result.pageSize()),
                () -> assertEquals(5L, result.totalElements()),
                () -> assertEquals(3, result.totalPages()),
                () -> assertTrue(result.hasNext()),
                () -> assertFalse(result.isLast())
        );
    }

    @Test
    void list_InvalidSortField_ThrowsInvalidSortFieldException() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by("postCode"));

        // when
        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class,
                () -> institutionService.list(pageable));

        // then
        // kolejność dozwolonych pól pochodzi z Set.of(), więc sprawdzamy tylko początek komunikatu
        assertTrue(exception.getMessage().startsWith("Niedozwolone pole sortowania: postCode. Dozwolone pola: "),
                exception.getMessage());
        verify(institutionRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void list_ValidSortField_PassesSortToRepository() {
        // given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "name"));
        when(institutionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                institution(1L, "Przychodnia Zdrowie")), pageable, 1));

        // when
        PageDto<InstitutionDto> result = institutionService.list(pageable);

        // then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(institutionRepository).findAll(captor.capture());
        assertAll(
                () -> assertEquals(Sort.by(Sort.Direction.DESC, "name"), captor.getValue().getSort()),
                () -> assertEquals(1, result.content().size()),
                () -> assertEquals("Przychodnia Zdrowie", result.content().get(0).name())
        );
    }

    @Test
    void getById_InstitutionExists_ReturnsInstitutionDto() {
        // given
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution(1L, "Przychodnia Zdrowie")));

        // when
        InstitutionDto result = institutionService.getById(1L);

        // then
        assertAll(
                () -> assertEquals(1L, result.id()),
                () -> assertEquals("Przychodnia Zdrowie", result.name()),
                () -> assertEquals("Warszawa", result.city()),
                () -> assertEquals("00-950", result.postCode()),
                () -> assertEquals("Marszałkowska", result.street()),
                () -> assertEquals("10", result.buildingNumber())
        );
    }

    @Test
    void getById_InstitutionNotFound_ThrowsInstitutionNotFoundException() {
        // given
        when(institutionRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        InstitutionNotFoundException exception = assertThrows(InstitutionNotFoundException.class, () -> institutionService.getById(1L));

        // then
        assertEquals("Nie znaleziono placówki: 1", exception.getMessage());
    }

    @Test
    void save_NameFree_SavesInstitution() {
        // given
        CreateInstitutionCommand command = new CreateInstitutionCommand("Szpital Miejski", "Gdańsk", "80-210", "Grunwaldzka", "100");
        when(institutionRepository.existsByNameIgnoreCase("Szpital Miejski")).thenReturn(false);
        when(institutionRepository.save(any(Institution.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        InstitutionDto result = institutionService.save(command);

        // then
        assertAll(
                () -> assertEquals("Szpital Miejski", result.name()),
                () -> assertEquals("Gdańsk", result.city()),
                () -> assertEquals("80-210", result.postCode()),
                () -> assertEquals("Grunwaldzka", result.street()),
                () -> assertEquals("100", result.buildingNumber())
        );
        verify(institutionRepository).save(any(Institution.class));
    }

    @Test
    void save_NameTaken_ThrowsInstitutionNameAlreadyTakenException() {
        // given
        CreateInstitutionCommand command = new CreateInstitutionCommand("Szpital Miejski", "Gdańsk", "80-210", "Grunwaldzka", "100");
        when(institutionRepository.existsByNameIgnoreCase("Szpital Miejski")).thenReturn(true);

        // when
        InstitutionNameAlreadyTakenException exception = assertThrows(InstitutionNameAlreadyTakenException.class,
                () -> institutionService.save(command));

        // then
        assertEquals("Placówka o nazwie Szpital Miejski już istnieje", exception.getMessage());
        verify(institutionRepository, never()).save(any(Institution.class));
    }

    @Test
    void update_NewNameFree_UpdatesInstitution() {
        // given
        Institution institution = institution(1L, "Przychodnia Zdrowie");
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Przychodnia Zdrowie Plus", "Warszawa", "00-950", "Marszałkowska", "12");
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(institutionRepository.existsByNameIgnoreCase("Przychodnia Zdrowie Plus")).thenReturn(false);
        when(institutionRepository.save(institution)).thenReturn(institution);

        // when
        InstitutionDto result = institutionService.update(1L, command);

        // then
        assertAll(
                () -> assertEquals("Przychodnia Zdrowie Plus", result.name()),
                () -> assertEquals("12", result.buildingNumber())
        );
        verify(institutionRepository).save(institution);
    }

    @Test
    void update_SameName_DoesNotCheckNameAvailability() {
        // given
        Institution institution = institution(1L, "Przychodnia Zdrowie");
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Przychodnia Zdrowie", "Kraków", "31-010", "Długa", "5");
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(institutionRepository.save(institution)).thenReturn(institution);

        // when
        InstitutionDto result = institutionService.update(1L, command);

        // then
        assertEquals("Kraków", result.city());
        verify(institutionRepository, never()).existsByNameIgnoreCase(anyString());
    }

    @Test
    void update_SameNameDifferentCase_UpdatesWithoutNameConflict() {
        // given
        Institution institution = institution(1L, "Przychodnia Zdrowie");
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("PRZYCHODNIA ZDROWIE", "Warszawa", "00-950", "Marszałkowska", "10");
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(institutionRepository.existsByNameIgnoreCase("PRZYCHODNIA ZDROWIE")).thenReturn(true); // znajduje samą siebie
        when(institutionRepository.save(institution)).thenReturn(institution);

        // when
        InstitutionDto result = institutionService.update(1L, command);

        // then
        assertEquals("PRZYCHODNIA ZDROWIE", result.name());
        verify(institutionRepository).save(institution);
    }

    @Test
    void update_NewNameTaken_ThrowsInstitutionNameAlreadyTakenException() {
        // given
        Institution institution = institution(1L, "Przychodnia Zdrowie");
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Centrum Medyczne Vita", "Warszawa", "00-950", "Marszałkowska", "10");
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(institutionRepository.existsByNameIgnoreCase("Centrum Medyczne Vita")).thenReturn(true);

        // when
        InstitutionNameAlreadyTakenException exception = assertThrows(InstitutionNameAlreadyTakenException.class,
                () -> institutionService.update(1L, command));

        // then
        assertEquals("Placówka o nazwie Centrum Medyczne Vita już istnieje", exception.getMessage());
        assertEquals("Przychodnia Zdrowie", institution.getName());
        verify(institutionRepository, never()).save(any(Institution.class));
    }

    @Test
    void update_InstitutionNotFound_ThrowsInstitutionNotFoundException() {
        // given
        UpdateInstitutionCommand command = new UpdateInstitutionCommand("Przychodnia Zdrowie Plus", "Warszawa", "00-950", "Marszałkowska", "12");
        when(institutionRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        InstitutionNotFoundException exception = assertThrows(InstitutionNotFoundException.class,
                () -> institutionService.update(1L, command));

        // then
        assertEquals("Nie znaleziono placówki: 1", exception.getMessage());
        verify(institutionRepository, never()).save(any(Institution.class));
    }

    @Test
    void delete_NoDoctorsAndNoVisits_DeletesInstitution() {
        // given
        Institution institution = institution(1L, "Przychodnia Zdrowie");
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));
        when(visitRepository.existsByInstitutionId(1L)).thenReturn(false);

        // when
        institutionService.delete(1L);

        // then
        verify(institutionRepository).delete(institution);
    }

    @Test
    void delete_InstitutionNotFound_ThrowsInstitutionNotFoundException() {
        // given
        when(institutionRepository.findById(1L)).thenReturn(Optional.empty());

        // when
        InstitutionNotFoundException exception = assertThrows(InstitutionNotFoundException.class, () -> institutionService.delete(1L));

        // then
        assertEquals("Nie znaleziono placówki: 1", exception.getMessage());
        verify(institutionRepository, never()).delete(any(Institution.class));
    }

    @Test
    void delete_InstitutionHasDoctors_ThrowsInstitutionAlreadyAssignedException() {
        // given
        Institution institution = institution(1L, "Przychodnia Zdrowie");
        Doctor doctor = Doctor.from(new CreateDoctorCommand(5L, "Ewa", "Zielińska", "600100200"));
        doctor.assignInstitution(institution);
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution));

        // when
        InstitutionAlreadyAssignedException exception = assertThrows(InstitutionAlreadyAssignedException.class,
                () -> institutionService.delete(1L));

        // then
        assertEquals("Placówka 1 ma przypisanych lekarzy", exception.getMessage());
        verify(institutionRepository, never()).delete(any(Institution.class));
    }

    @Test
    void delete_InstitutionHasVisits_ThrowsInstitutionAlreadyAssignedException() {
        // given
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution(1L, "Przychodnia Zdrowie")));
        when(visitRepository.existsByInstitutionId(1L)).thenReturn(true);

        // when
        InstitutionAlreadyAssignedException exception = assertThrows(InstitutionAlreadyAssignedException.class,
                () -> institutionService.delete(1L));

        // then
        assertEquals("Placówka 1 ma wizyty", exception.getMessage());
        verify(institutionRepository, never()).delete(any(Institution.class));
    }

    // Encje nie mają setId(), a from() zostawia id = null.
    private static Institution institution(Long id, String name) {
        Institution institution = Institution.from(new CreateInstitutionCommand(name, "Warszawa", "00-950", "Marszałkowska", "10"));
        ReflectionTestUtils.setField(institution, "id", id);
        return institution;
    }
}
