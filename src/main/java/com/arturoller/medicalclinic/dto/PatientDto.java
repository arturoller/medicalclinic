package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Email;

import java.time.LocalDate;

public record PatientDto(
        Integer id,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday,
        Integer userId
) {
}
