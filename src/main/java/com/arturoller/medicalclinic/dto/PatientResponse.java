package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.PatientModel;

import java.time.LocalDate;

public record PatientResponse(
        String email,
        String idCardNo,
        String firstName,
        String lastName,
        String phoneNumber,
        LocalDate birthday
) {

    public static PatientResponse from(PatientModel patientModel) {
        return new PatientResponse(
                patientModel.getEmail(),
                patientModel.getIdCardNo(),
                patientModel.getFirstName(),
                patientModel.getLastName(),
                patientModel.getPhoneNumber(),
                patientModel.getBirthday()
        );
    }

}
