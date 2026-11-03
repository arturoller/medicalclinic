package com.arturoller.medicalclinic.util;

import com.arturoller.medicalclinic.model.Email;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.Locale;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailUtil {
    public static String clean(String email) {
        Objects.requireNonNull(email, "Email cannot be null");
        return email.replace('\u00A0', ' ').strip().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String email) {
        EmailValidator emailValidator = EmailValidator.getInstance();
        return emailValidator.isValid(email);
    }

    public static boolean isValid(Email email) {
        return isValid(email.toString());
    }
}
