package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Email;

import java.time.LocalDate;

public record UpdateUserCommand(
        Email email
) {
}
