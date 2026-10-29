package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.PatientCreateRequest;
import com.arturoller.medicalclinic.dto.PatientResponse;
import com.arturoller.medicalclinic.dto.PatientUpdateRequest;
import com.arturoller.medicalclinic.exceptions.PatientAlreadyExistsException;
import com.arturoller.medicalclinic.exceptions.PatientNotFoundException;
import com.arturoller.medicalclinic.model.PatientModel;
import com.arturoller.medicalclinic.repisitory.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public List<PatientModel> getAllPatients() {
        return patientRepository.findAllPatients();
    }

    public PatientModel getPatientByEmail(String email) {
        return patientRepository.getPatientByEmail(email)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + email));
    }

    public PatientModel addPatient(PatientModel patientModel) {
        if(patientRepository.getPatientByEmail(patientModel.getEmail()).isPresent()) {
            throw new PatientAlreadyExistsException("Patient already exists");
        }

        return patientRepository.addPatient(patientModel);
    }

    public PatientResponse createPatient(PatientCreateRequest createRequest) {
        if(patientRepository.getPatientByEmail(createRequest.email()).isPresent()) {
            throw new PatientAlreadyExistsException("Patient already exists: "  + createRequest.email());
        }

        PatientModel patientModel = patientRepository.addPatient(createRequest.toEntity());

        return PatientResponse.from(patientModel);
    }

    public PatientResponse updatePatient(String email, PatientUpdateRequest createRequest) {
        PatientModel patientModel = getPatientByEmail(email);

        patientModel.setEmail(createRequest.email());
        patientModel.setPassword(createRequest.password());
        patientModel.setIdCardNo(createRequest.idCardNo());
        patientModel.setFirstName(createRequest.firstName());
        patientModel.setLastName(createRequest.lastName());
        patientModel.setPhoneNumber(createRequest.phoneNumber());
        patientModel.setBirthday(createRequest.birthday());

        return PatientResponse.from(patientModel);
    }

    public void deletePatient(String email) {
        if(patientRepository.getPatientByEmail(email).isEmpty()) {
            throw new PatientAlreadyExistsException("Patient not found: " + email);
        }

        patientRepository.deletePatient(email);
    }
}
