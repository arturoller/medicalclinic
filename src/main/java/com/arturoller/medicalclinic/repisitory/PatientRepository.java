package com.arturoller.medicalclinic.repisitory;

import com.arturoller.medicalclinic.exceptions.PatientNotFoundException;
import com.arturoller.medicalclinic.model.PatientModel;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PatientRepository {

    private final Map<String, PatientModel> patients = new ConcurrentHashMap<>();

    public List<PatientModel> findAllPatients() {
        return List.copyOf(patients.values());
    }

    public Optional<PatientModel> getPatientByEmail(String email) {
        return Optional.ofNullable(patients.get(email));
    }

    public PatientModel addPatient(PatientModel patient) {
        patients.put(patient.getEmail(), patient);

        return patient;
    }

    public void deletePatient(String email) {
        patients.remove(email);
    }
}
