package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Patient {
    private Email email;

    @Setter(AccessLevel.NONE)
    private String passwordHash;

    private final String idCardNo;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthday;

    public void update(UpdatePatientCommand command) {
        setEmail(command.email());
        setFirstName(command.firstName());
        setLastName(command.lastName());
        setPhoneNumber(command.phoneNumber());
        setBirthday(command.birthday());
    }

    public static Patient from(CreatePatientCommand command, String passwordHash) {
        return new Patient(command.email(), passwordHash, command.idCardNo(), command.firstName(), command.lastName(), command.phoneNumber(), command.birthday());
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }
}
