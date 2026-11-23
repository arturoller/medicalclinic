package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyAssignedException extends MedicalClinicException {
    public UserAlreadyAssignedException(String message, HttpStatus status) {
        super(message, status);
    }
}
