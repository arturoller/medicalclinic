package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class InstitutionNotAssignedException extends MedicalClinicException {
    public InstitutionNotAssignedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
