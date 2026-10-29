package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.PatientCreateRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class PatientModel {

    private String email;
    private String password;
    private String idCardNo;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthday;

    public String getPassword() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static PatientModel from(PatientCreateRequest patientCreateRequest) {
        return new PatientModel(
                patientCreateRequest.email(),
                patientCreateRequest.password(),
                patientCreateRequest.idCardNo(),
                patientCreateRequest.firstName(),
                patientCreateRequest.lastName(),
                patientCreateRequest.phoneNumber(),
                patientCreateRequest.birthday()
        );
    }

}
