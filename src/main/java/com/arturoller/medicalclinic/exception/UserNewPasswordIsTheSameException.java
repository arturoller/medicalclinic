package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class UserNewPasswordIsTheSameException extends MedicalClinicException {
    public UserNewPasswordIsTheSameException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
