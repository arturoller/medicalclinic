package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.exception.EmailValidationException;
import com.arturoller.medicalclinic.util.EmailUtil;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.http.HttpStatus;

public record Email(@JsonValue String email) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Email {
        email = EmailUtil.clean(email);
        if (!EmailUtil.isValid(email)) {
            throw new EmailValidationException("The provided email address is invalid: " + email, HttpStatus.BAD_REQUEST);
        }
    }
}
