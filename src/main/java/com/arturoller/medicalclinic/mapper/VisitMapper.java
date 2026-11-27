package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.VisitDto;
import com.arturoller.medicalclinic.model.Visit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VisitMapper {
    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "institutionId", source = "institution.id")
    @Mapping(target = "patientId", source = "patient.id")
    VisitDto toDto(Visit visit);
}
