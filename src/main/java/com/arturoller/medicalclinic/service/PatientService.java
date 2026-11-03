package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exceptions.EmailAlreadyTakenException;
import com.arturoller.medicalclinic.exceptions.PatientAlreadyExistsException;
import com.arturoller.medicalclinic.exceptions.PatientCurrentPasswordIsTheSame;
import com.arturoller.medicalclinic.exceptions.PatientNotFoundException;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.repisitory.PatientRepository;
import com.arturoller.medicalclinic.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

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
        return patientRepository.findByEmail(email)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + email));
    }

    public PatientDto create(CreatePatientCommand command) {
        if (patientRepository.exists(command.email())) {
            throw new PatientAlreadyExistsException("Patient already exists: " + command.email());
        }
        Patient patient = patientRepository.create(patientMapper.toModel(command));

        return patientMapper.toDto(patient);
    }

    public PatientDto update(Email email, UpdatePatientCommand command) {
        Patient patient = findByEmail(email);
        if (!email.equals(command.email()) && patientRepository.exists(command.email())) {
            throw new EmailAlreadyTakenException("Email already exists: " + command.email());
        }
        patient.update(command);
        patientRepository.update(email, patient);

        return patientMapper.toDto(patient);
    }

    public void delete(Email email) {
        if (!patientRepository.delete(email)) {
            throw new PatientNotFoundException("Patient not found: " + email);
        }
    }

    public void changePassword(Email email, ChangePasswordPatientCommand command) {
        Patient patient = findByEmail(email);
        if (patient.passwordMatches(command.password(), passwordEncoder)) {
            throw new PatientCurrentPasswordIsTheSame("Current password is the same! Can't change password!");
        }
        patient.changePassword(command, passwordEncoder);
    }


}
