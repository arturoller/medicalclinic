package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class VisitNotBookedByPatientException extends MedicalClinicException {
    public VisitNotBookedByPatientException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
