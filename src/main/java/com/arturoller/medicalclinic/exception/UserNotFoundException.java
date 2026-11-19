package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends MedicalClinicException {
    public UserNotFoundException(String message, HttpStatus status) {
        super(message, status);
    }
}
