package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;

import java.time.LocalDate;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;

import java.time.LocalDate;

public record PatientDto(
        String email,
        String idCardNo,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday
) {
}
