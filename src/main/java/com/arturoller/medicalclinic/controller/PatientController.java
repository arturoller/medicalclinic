package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.model.Email;
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
    public List<PatientDto> getAll() {
        return patientService.getAll().stream()
                .map(PatientDto::from)
                .toList();
    }

    @GetMapping("/{email}")
    public PatientDto findByEmail(@PathVariable Email email) {
        return PatientDto.from(patientService.findByEmail(email));
    }

    @PatchMapping("/{email}")
    public PatientDto update(@PathVariable Email email, @RequestBody UpdatePatientCommand createRequest) {
        return patientService.update(email, createRequest);
    }

    @PatchMapping("/{email}/password")
    public void changePassword(@PathVariable Email email, @RequestBody ChangePasswordPatientCommand command) {
        patientService.changePassword(email, command);
    }

    @DeleteMapping("/{email}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Email email) {
        patientService.delete(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDto create(@RequestBody CreatePatientCommand createRequest) {
        return patientService.create(createRequest);
    }
}
