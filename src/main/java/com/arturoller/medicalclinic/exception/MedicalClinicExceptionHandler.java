package com.arturoller.medicalclinic.exception;

import com.arturoller.medicalclinic.dto.ErrorMessageDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MedicalClinicExceptionHandler {
    @ExceptionHandler(MedicalClinicException.class)
    public ResponseEntity<ErrorMessageDto> handleException(MedicalClinicException e) {
        return ResponseEntity.status(e.getStatus().value()).body(new ErrorMessageDto(e.getMessage(), e.getStatus()));
    }
}
