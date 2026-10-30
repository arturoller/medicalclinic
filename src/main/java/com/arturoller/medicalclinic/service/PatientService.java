package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.exceptions.PatientAlreadyExistsException;
import com.arturoller.medicalclinic.exceptions.PatientNotFoundException;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.repisitory.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;

    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    public Patient findByEmail(String email) {
        return patientRepository.findByEmail(email)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + email));
    }

    public PatientDto create(CreatePatientCommand command) {
        if (patientRepository.exists(command.email())) {
            throw new PatientAlreadyExistsException("Patient already exists: " + command.email());
        }
        Patient patient = patientRepository.create(command.toEntity());
        return PatientDto.from(patient);
    }

    public PatientDto update(String email, UpdatePatientCommand command) {
        Patient patient = findByEmail(email);
        patient.update(command);

        return PatientDto.from(patient);
    }

    public void delete(String email) {
        if (!patientRepository.delete(email)) {
            throw new PatientNotFoundException("Patient not found: " + email);
        }
    }
}
