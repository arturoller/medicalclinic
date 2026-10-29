package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.PatientModel;

import java.time.LocalDate;

public record PatientCreateRequest(
        String email,
        String password,
        String idCardNo,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday
) {

    public PatientModel toEntity() {
        return new PatientModel(
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