package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class PatientAlreadyAssignedException extends MedicalClinicException {
    public PatientAlreadyAssignedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
