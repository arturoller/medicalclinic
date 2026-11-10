package com.arturoller.medicalclinic.dto;

public record ChangePasswordPatientCommand(
        String currentPassword,
        String newPassword
) {
}
