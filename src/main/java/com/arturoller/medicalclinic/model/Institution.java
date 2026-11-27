package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreateInstitutionCommand;
import com.arturoller.medicalclinic.dto.UpdateInstitutionCommand;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "institutions")
public class Institution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String city;
    private String postCode;
    private String street;
    private String buildingNumber;

    @ManyToMany(mappedBy = "institutions")
    @Setter(AccessLevel.NONE)
    private Set<Doctor> doctors = new HashSet<>();

    public void update(UpdateInstitutionCommand command) {
        setName(command.name());
        setCity(command.city());
        setPostCode(command.postCode());
        setStreet(command.street());
        setBuildingNumber(command.buildingNumber());
    }

    public static Institution from(CreateInstitutionCommand command) {
        return new Institution(null, command.name(), command.city(), command.postCode(), command.street(), command.buildingNumber(), new HashSet<>());
    }
}
