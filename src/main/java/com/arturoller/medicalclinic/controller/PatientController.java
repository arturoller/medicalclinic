package com.arturoller.medicalclinic.controller;

import com.arturoller.medicalclinic.dto.ChangePasswordUserCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import com.arturoller.medicalclinic.mapper.PatientMapper;
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
        return patientService.getAll();
    }

    @GetMapping("/{patienId}")
    public PatientDto getById(@PathVariable Integer patienId) {
        return patientService.getById(patienId);
    }

    @PutMapping("/{patienId}")
    public PatientDto update(@PathVariable Integer patienId, @RequestBody UpdatePatientCommand command) {
        return patientService.update(patienId, command);
    }

    @DeleteMapping("/{patienId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer patienId) {
        patientService.delete(patienId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientDto create(@RequestBody CreatePatientCommand command) {
        return patientService.create(command);
    }
}
