package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;

    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    public Patient findByEmail(Email email) {
        return patientRepository.findByEmail(email).orElseThrow(() -> new PatientNotFoundException("Patient not found: " + email, HttpStatus.NOT_FOUND));
    }

    public PatientDto create(CreatePatientCommand command) {
        if (patientRepository.exists(command.email())) {
            throw new PatientAlreadyExistsException("Patient already exists: " + command.email(), HttpStatus.CONFLICT);
        }
        Patient patient = patientRepository.create(patientMapper.toModel(command));

        return patientMapper.toDto(patient);
    }

    public PatientDto update(Email email, UpdatePatientCommand command) {
        Patient patient = findByEmail(email);
        if (!email.equals(command.email()) && patientRepository.exists(command.email())) {
            throw new EmailAlreadyTakenException("Email already exists: " + command.email(), HttpStatus.CONFLICT);
        }
        patient.update(command);
        patientRepository.update(email, patient);

        return patientMapper.toDto(patient);
    }

    public void delete(Email email) {
        if (!patientRepository.delete(email)) {
            throw new PatientNotFoundException("Patient not found: " + email, HttpStatus.NOT_FOUND);
        }
    }

    public void changePassword(Email email, ChangePasswordPatientCommand command) {
        Patient patient = findByEmail(email);
        if (!passwordEncoder.matches(command.currentPassword(), patient.getPasswordHash())) {
            throw new PatientCurrentPasswordDoesNotMatch("Current password does not match!", HttpStatus.UNAUTHORIZED);
        }
        if (passwordEncoder.matches(command.newPassword(), patient.getPasswordHash())) {
            throw new PatientCurrentPasswordIsTheSame("New password must differ from current one!", HttpStatus.CONFLICT);
        }
        patient.changePassword(passwordEncoder.encode(command.newPassword()));
        patientRepository.update(email, patient);
    }
}
