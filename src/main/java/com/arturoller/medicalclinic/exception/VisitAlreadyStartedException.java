package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class VisitAlreadyStartedException extends MedicalClinicException {
    public VisitAlreadyStartedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
