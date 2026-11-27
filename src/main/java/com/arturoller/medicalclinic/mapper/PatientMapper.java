package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    @Mapping(target = "userId", source = "user.id")
    PatientDto toDto(Patient patient);

    default Patient toModel(CreatePatientCommand command) {
        return Patient.from(command);
    }
}
