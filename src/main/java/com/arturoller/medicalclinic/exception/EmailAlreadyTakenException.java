package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyTakenException extends MedicalClinicException {
    public EmailAlreadyTakenException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
