package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Patient;

import java.time.LocalDate;

public record UpdatePatientCommand(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday
) {
}

