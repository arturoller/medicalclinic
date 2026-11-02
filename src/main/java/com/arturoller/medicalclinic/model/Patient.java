package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class Patient {
    private Email email;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private String password;

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

    public static Patient from(CreatePatientCommand command) {
        return new Patient(
                command.email(),
                command.password(),
                command.idCardNo(),
                command.firstName(),
                command.lastName(),
                command.phoneNumber(),
                command.birthday()
        );
    }

    public boolean passwordEquals(String rawPassword) {
        return password.equals(rawPassword);
    }

    public void changePassword(ChangePasswordPatientCommand command) {
        password = command.password();
    }
}
