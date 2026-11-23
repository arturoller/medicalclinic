package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserCurrentPasswordDoesNotMatch extends MedicalClinicException {
    public UserCurrentPasswordDoesNotMatch(String message, HttpStatus status) {
        super(message, status);
    }
}
