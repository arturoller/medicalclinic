package com.arturoller.medicalclinic.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Locale;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailUtil {
    public static String clean(String email) {
        Objects.requireNonNull(email, "Email cannot be null");
        return email.replace('\u00A0', ' ').strip().toLowerCase(Locale.ROOT);
    }
}
