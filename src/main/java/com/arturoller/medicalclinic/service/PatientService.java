package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.ChangePasswordUserCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    @Transactional(readOnly = true)
    public List<PatientDto> getAll() {
        return patientRepository.findAll().stream()
                .map(patientMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientDto getById(Integer patientId) {
        return patientMapper.toDto(findPatientById(patientId));
    }

    @Transactional
    public PatientDto create(CreatePatientCommand command) {
        User user = findFreeUser(command.userId());
        Patient patient = patientMapper.toModel(command);
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Transactional
    public PatientDto update(Integer patientId, UpdatePatientCommand command) {
        Patient patient = findPatientById(patientId);
        patient.update(command);
        if (command.userId() != null && !command.userId().equals(patient.getUser().getId())) {
            patient.setUser(findFreeUser(command.userId()));
        }
        return patientMapper.toDto(patientRepository.save(patient));
    }

    @Transactional
    public void delete(Integer patientId) {
        Patient patient = findPatientById(patientId);
        patientRepository.delete(patient);
    }

    private User findFreeUser(Integer userId) {
        if (userId == null) {
            throw new UserRequiredException("Patient must be assigned to a user", HttpStatus.BAD_REQUEST);
        }
        User user = findUserById(userId);
        if (patientRepository.existsByUserId(userId)) {
            throw new UserAlreadyAssignedException("User already assigned to another patient: " + userId, HttpStatus.CONFLICT);
        }
        return user;
    }

    private Patient findPatientById(Integer patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + patientId, HttpStatus.NOT_FOUND));
    }

    private User findUserById(Integer patientId) {
        return userRepository.findById(patientId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + patientId, HttpStatus.NOT_FOUND));
    }
}
