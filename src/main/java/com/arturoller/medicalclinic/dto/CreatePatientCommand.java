package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.util.EmailUtil;

import java.time.LocalDate;

public record CreatePatientCommand(
        Email email,
        String password,
        String idCardNo,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday
) {
}
