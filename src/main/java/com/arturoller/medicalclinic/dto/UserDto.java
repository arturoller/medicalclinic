package com.arturoller.medicalclinic.dto;

import com.arturoller.medicalclinic.model.Email;

public record UserDto(
        Integer id,
        Email email,
        String idCardNo
) {
}