package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.util.EmailUtil;

public record Email(String email) {
    public Email {
        email = EmailUtil.clean(email);
    }

    @Override
    public String toString() {
        return email;
    }
}
