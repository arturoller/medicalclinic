package com.arturoller.medicalclinic.repisitory;

import com.arturoller.medicalclinic.model.Patient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PatientRepository {
    private final Map<String, Patient> patients = new ConcurrentHashMap<>();

    public List<Patient> findAll() {
        return List.copyOf(patients.values());
    }

    public Optional<Patient> findByEmail(String email) {
        return Optional.ofNullable(patients.get(email));
    }

    public boolean exists(String email) {
        return patients.containsKey(email);
    }

    public Patient create(Patient patient) {
        patients.put(patient.getEmail(), patient);

        return patient;
    }

    public boolean delete(String email) {
        return patients.remove(email) != null;
    }
}
