package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreateDoctorCommand;
import com.arturoller.medicalclinic.dto.UpdateDoctorCommand;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(
            name = "doctors_patients",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "patient_id")
    )
    @Setter(AccessLevel.NONE)
    private Set<Patient> patients = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "doctors_institutions",
            joinColumns = @JoinColumn(name = "doctor_id"),
            inverseJoinColumns = @JoinColumn(name = "institution_id")
    )
    @Setter(AccessLevel.NONE)
    private Set<Institution> institutions = new HashSet<>();

    public void assignPatient(Patient patient) {
        patients.add(patient);
        patient.getDoctors().add(this);
    }

    public void unassignPatient(Patient patient) {
        patients.remove(patient);
        patient.getDoctors().remove(this);
    }

    public boolean hasPatient(Patient patient) {
        return patients.contains(patient);
    }

    public void assignInstitution(Institution institution) {
        institutions.add(institution);
        institution.getDoctors().add(this);
    }

    public void unassignInstitution(Institution institution) {
        institutions.remove(institution);
        institution.getDoctors().remove(this);
    }

    public boolean hasInstitution(Institution institution) {
        return institutions.contains(institution);
    }

    public void update(UpdateDoctorCommand command) {
        setFirstName(command.firstName());
        setLastName(command.lastName());
        setPhoneNumber(command.phoneNumber());
    }

    public static Doctor from(CreateDoctorCommand command) {
        return new Doctor(null, command.firstName(), command.lastName(), command.phoneNumber(), null, new HashSet<>(), new HashSet<>());
    }
}
