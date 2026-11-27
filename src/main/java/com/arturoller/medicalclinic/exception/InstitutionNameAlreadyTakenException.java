package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class InstitutionNameAlreadyTakenException extends MedicalClinicException {
    public InstitutionNameAlreadyTakenException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
