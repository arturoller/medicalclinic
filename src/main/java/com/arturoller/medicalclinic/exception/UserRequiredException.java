package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserRequiredException extends MedicalClinicException {
    public UserRequiredException(String message, HttpStatus status) {
        super(message, status);
    }
}
