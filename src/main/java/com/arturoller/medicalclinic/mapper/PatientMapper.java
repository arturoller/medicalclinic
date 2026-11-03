package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.PatientDto;
import com.arturoller.medicalclinic.model.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    @Mapping(source = "email.email", target = "email")
    PatientDto toDto(Patient patient);

    Patient toModel(CreatePatientCommand patientDto);
}
