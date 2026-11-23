package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class PatientNotFoundException extends MedicalClinicException {
    public PatientNotFoundException(String message, HttpStatus status) {
        super(message, status);
    }
}
