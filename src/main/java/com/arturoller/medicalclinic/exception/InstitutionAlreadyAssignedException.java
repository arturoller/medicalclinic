package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class InstitutionAlreadyAssignedException extends MedicalClinicException {
    public InstitutionAlreadyAssignedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
