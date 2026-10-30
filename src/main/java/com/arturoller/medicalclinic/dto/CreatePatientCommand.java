package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Patient;

import java.time.LocalDate;

public record CreatePatientCommand(
        String email,
        String password,
        String idCardNo,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday
) {
    public Patient toEntity() {
        return new Patient(
                email,
                password,
                idCardNo,
                firstName,
                lastName,
                phoneNumber,
                birthday
        );
    }
}