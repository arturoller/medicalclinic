package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.CreateDoctorCommand;
import com.arturoller.medicalclinic.dto.DoctorDto;
import com.arturoller.medicalclinic.model.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoctorMapper {
    @Mapping(target = "userId", source = "user.id")
    DoctorDto toDto(Doctor doctor);

    default Doctor toModel(CreateDoctorCommand command) {
        return Doctor.from(command);
    }
}
