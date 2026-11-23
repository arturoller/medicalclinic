package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

abstract public class MedicalClinicException extends RuntimeException {
    private final HttpStatus status;

    public MedicalClinicException(String message, HttpStatus httpStatus) {
        super(message);
        this.status = httpStatus;
    }

    public MedicalClinicException(String message) {
        super(message);
        this.status = null;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
