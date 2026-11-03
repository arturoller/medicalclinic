package com.arturoller.medicalclinic.exceptions;

public class PatientCurrentPasswordIsTheSame extends RuntimeException {
    public PatientCurrentPasswordIsTheSame(String message) {
        super(message);
    }
}
