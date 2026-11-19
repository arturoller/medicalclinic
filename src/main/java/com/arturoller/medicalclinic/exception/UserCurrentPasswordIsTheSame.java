package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserCurrentPasswordIsTheSame extends MedicalClinicException {
    public UserCurrentPasswordIsTheSame(String message, HttpStatus status) {
        super(message, status);
    }
}
