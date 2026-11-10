package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.exceptions.InvalidEmailException;
import com.arturoller.medicalclinic.util.EmailUtil;

public record Email(String email) {
    public Email {
        email = EmailUtil.clean(email);
        if (!EmailUtil.isValid(email)) {
            throw new InvalidEmailException("The provided email address is invalid: " + email);
        }
    }
}
