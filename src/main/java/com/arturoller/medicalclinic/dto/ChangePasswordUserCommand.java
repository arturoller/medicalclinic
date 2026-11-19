package com.arturoller.medicalclinic.dto;

public record ChangePasswordUserCommand(
        String currentPassword,
        String newPassword
) {
}
