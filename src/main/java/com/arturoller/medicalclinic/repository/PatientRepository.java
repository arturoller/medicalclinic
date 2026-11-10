package com.arturoller.medicalclinic.repository;

import com.arturoller.medicalclinic.exception.EmailAlreadyTakenException;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PatientRepository {
    private final Map<Email, Patient> patients = new ConcurrentHashMap<>();

    public List<Patient> findAll() {
        return List.copyOf(patients.values());
    }

    public Optional<Patient> findByEmail(Email email) {
        return Optional.ofNullable(patients.get(email));
    }

    public boolean exists(Email email) {
        return patients.containsKey(email);
    }

    public Patient create(Patient patient) {
        patients.put(patient.getEmail(), patient);

        return patient;
    }

    public boolean delete(Email email) {
        return patients.remove(email) != null;
    }

    public Patient update(Email oldEmail, Patient patient) {
        Email newEmail = patient.getEmail();
        if (!oldEmail.equals(newEmail)) {
            if (patients.putIfAbsent(newEmail, patient) != null) {
                throw new EmailAlreadyTakenException("Email already exists: "  + newEmail, HttpStatus.CONFLICT);
            }
            patients.remove(oldEmail);
        }
        return patient;
    }
}
