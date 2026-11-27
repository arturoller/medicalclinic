package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

abstract public class MedicalClinicException extends RuntimeException {
    private final HttpStatus status;

    public MedicalClinicException(String message, HttpStatus httpStatus) {
        super(message);
        this.status = httpStatus;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
