package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.UserRepository;
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
public class PatientService {
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final VisitRepository visitRepository;
    private final PatientMapper patientMapper;

    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "firstName", "lastName", "birthday");
    private final SortValidator sortValidator;

    @Transactional(readOnly = true)
    public PageDto<PatientDto> list(Pageable pageable) {
        sortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);
        Page<PatientDto> content = patientRepository.findAll(pageable)
                .map(patientMapper::toDto);
        return PageDto.from(content);
    }

    @Transactional(readOnly = true)
    public PatientDto getById(Long patientId) {
        return patientMapper.toDto(findPatientById(patientId));
    }

    @Transactional
    public PatientDto create(CreatePatientCommand command) {
        User user = findUserById(command.userId());
        if (patientRepository.existsByUserId(command.userId())) {
            throw new UserAlreadyAssignedException("Użytkownik " + command.userId() + " jest już przypisany do innego pacjenta");
        }
        Patient patient = patientMapper.toModel(command);
        patient.setUser(user);
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Transactional
    public PatientDto update(Long patientId, UpdatePatientCommand command) {
        Patient patient = findPatientById(patientId);
        patient.update(command);
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Transactional
    public void delete(Long patientId) {
        Patient patient = findPatientById(patientId);
        if (!patient.getDoctors().isEmpty()) {
            throw new PatientAlreadyAssignedException("Pacjent " + patientId + " jest przypisany do lekarza");
        }
        if (visitRepository.existsByPatientId(patientId)) {
            throw new PatientAlreadyAssignedException("Pacjent " + patientId + " ma wizyty");
        }
        patientRepository.delete(patient);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Nie znaleziono użytkownika: " + userId));
    }

    private Patient findPatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Nie znaleziono pacjenta: " + patientId));
    }
}
