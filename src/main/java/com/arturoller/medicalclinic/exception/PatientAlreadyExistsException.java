package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class PatientAlreadyExistsException extends MedicalClinicException {
    public PatientAlreadyExistsException(String message, HttpStatus status) {
        super(message, status);
    }
}
