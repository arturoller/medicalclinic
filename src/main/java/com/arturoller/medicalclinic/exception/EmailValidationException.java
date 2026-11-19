package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class EmailValidationException extends MedicalClinicException {
    public EmailValidationException(String message, HttpStatus status) {
        super(message, status);
    }
}
