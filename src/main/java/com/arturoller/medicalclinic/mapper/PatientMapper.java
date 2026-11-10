package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public abstract class PatientMapper {
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Mapping(source = "email.email", target = "email")
    public abstract PatientDto toDto(Patient patient);

    public Patient toModel(CreatePatientCommand command) {
        if (command == null) {
            return null;
        }

        return Patient.from(command, passwordEncoder.encode(command.password()));
    }
}
