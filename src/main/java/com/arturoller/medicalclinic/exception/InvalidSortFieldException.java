package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class InvalidSortFieldException extends MedicalClinicException {
    public InvalidSortFieldException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
