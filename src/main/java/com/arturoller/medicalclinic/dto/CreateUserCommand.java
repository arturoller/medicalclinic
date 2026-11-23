package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Email;

public record CreateUserCommand(
        Email email,
        String password,
        String idCardNo
) {
}
