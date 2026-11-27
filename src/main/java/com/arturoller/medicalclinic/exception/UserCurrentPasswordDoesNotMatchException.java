package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserCurrentPasswordDoesNotMatchException extends MedicalClinicException {
    public UserCurrentPasswordDoesNotMatchException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
