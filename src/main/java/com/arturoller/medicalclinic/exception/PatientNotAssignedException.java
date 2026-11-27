package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class PatientNotAssignedException extends MedicalClinicException {
    public PatientNotAssignedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
