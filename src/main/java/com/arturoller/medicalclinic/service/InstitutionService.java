package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.CreateInstitutionCommand;
import com.arturoller.medicalclinic.dto.InstitutionDto;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.UpdateInstitutionCommand;
import com.arturoller.medicalclinic.exception.InstitutionAlreadyAssignedException;
import com.arturoller.medicalclinic.exception.InstitutionNameAlreadyTakenException;
import com.arturoller.medicalclinic.exception.InstitutionNotFoundException;
import com.arturoller.medicalclinic.mapper.InstitutionMapper;
import com.arturoller.medicalclinic.model.Institution;
import com.arturoller.medicalclinic.repository.InstitutionRepository;
import com.arturoller.medicalclinic.repository.VisitRepository;
import com.arturoller.medicalclinic.util.SortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class InstitutionService {
    private final InstitutionRepository institutionRepository;
    private final VisitRepository visitRepository;
    private final InstitutionMapper institutionMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "name", "city");
    private final SortValidator sortValidator;

    @Transactional(readOnly = true)
    public PageDto<InstitutionDto> list(Pageable pageable) {
        sortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);
        Page<InstitutionDto> content = institutionRepository.findAll(pageable)
                .map(institutionMapper::toDto);
        return PageDto.from(content);
    }

    @Transactional(readOnly = true)
    public InstitutionDto getById(Long institutionId) {
        return institutionMapper.toDto(findInstitutionById(institutionId));
    }

    @Transactional
    public InstitutionDto save(CreateInstitutionCommand command) {
        if (institutionRepository.existsByNameIgnoreCase(command.name())) {
            throw new InstitutionNameAlreadyTakenException("Placówka o nazwie " + command.name() + " już istnieje");
        }
        Institution institution = institutionMapper.toModel(command);
        return institutionMapper.toDto(institutionRepository.save(institution));
    }

    @Transactional
    public InstitutionDto update(Long institutionId, UpdateInstitutionCommand command) {
        Institution institution = findInstitutionById(institutionId);
        if (!institution.getName().equalsIgnoreCase(command.name()) && institutionRepository.existsByNameIgnoreCase(command.name())) {
            throw new InstitutionNameAlreadyTakenException("Placówka o nazwie " + command.name() + " już istnieje");
        }
        institution.update(command);
        return institutionMapper.toDto(institutionRepository.save(institution));
    }

    @Transactional
    public void delete(Long institutionId) {
        Institution institution = findInstitutionById(institutionId);
        if (!institution.getDoctors().isEmpty()) {
            throw new InstitutionAlreadyAssignedException("Placówka " + institutionId + " ma przypisanych lekarzy");
        }
        if (visitRepository.existsByInstitutionId(institutionId)) {
            throw new InstitutionAlreadyAssignedException("Placówka " + institutionId + " ma wizyty");
        }
        institutionRepository.delete(institution);
    }

    private Institution findInstitutionById(Long institutionId) {
        return institutionRepository.findById(institutionId)
                .orElseThrow(() -> new InstitutionNotFoundException("Nie znaleziono placówki: " + institutionId));
    }
}
