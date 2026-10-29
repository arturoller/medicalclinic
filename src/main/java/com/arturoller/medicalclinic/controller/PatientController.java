package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.PatientCreateRequest;
import com.arturoller.medicalclinic.dto.PatientResponse;
import com.arturoller.medicalclinic.dto.PatientUpdateRequest;
import com.arturoller.medicalclinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping
    public List<PatientResponse> getAllPatients() {
        return patientService.getAllPatients().stream()
                .map(PatientResponse::from)
                .toList();
    }

    @GetMapping("/{email}")
    public PatientResponse getPatientByEmail(@PathVariable String email) {
        return PatientResponse.from(patientService.getPatientByEmail(email));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse createPatient(@RequestBody PatientCreateRequest createRequest) {
        return patientService.createPatient(createRequest);
    }

    @PatchMapping("/{email}")
    public PatientResponse updatePatient(@PathVariable String email, @RequestBody PatientUpdateRequest createRequest) {
        return patientService.updatePatient(email, createRequest);
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable String email) {
        patientService.deletePatient(email);
    }

}
