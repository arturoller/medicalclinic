package com.arturoller.medicalclinic.exception;

import org.springframework.http.HttpStatus;

public class DoctorAlreadyAssignedException extends MedicalClinicException {
    public DoctorAlreadyAssignedException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
