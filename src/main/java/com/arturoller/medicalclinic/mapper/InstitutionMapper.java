package com.arturoller.medicalclinic.mapper;

import com.arturoller.medicalclinic.dto.CreateInstitutionCommand;
import com.arturoller.medicalclinic.dto.InstitutionDto;
import com.arturoller.medicalclinic.model.Institution;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {
    InstitutionDto toDto(Institution institution);

    default Institution toModel(CreateInstitutionCommand command) {
        return Institution.from(command);
    }
}
