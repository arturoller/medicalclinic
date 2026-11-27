package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthday;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @ManyToMany(mappedBy = "patients")
    @Setter(AccessLevel.NONE)
    private Set<Doctor> doctors = new HashSet<>();

    public void update(UpdatePatientCommand command) {
        setFirstName(command.firstName());
        setLastName(command.lastName());
        setPhoneNumber(command.phoneNumber());
        setBirthday(command.birthday());
    }

    public static Patient from(CreatePatientCommand command) {
        return new Patient(null, command.firstName(), command.lastName(), command.phoneNumber(), command.birthday(), null, new HashSet<>());
    }
}
