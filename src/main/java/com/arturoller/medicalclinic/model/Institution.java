package com.arturoller.medicalclinic.model;

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
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    private String city;
    private String postCode;
    private String street;
    private String buildingNumber;

    @ManyToMany(mappedBy = "institutions")
    @Setter(AccessLevel.NONE)
    private Set<Doctor> doctors = new HashSet<>();
}
