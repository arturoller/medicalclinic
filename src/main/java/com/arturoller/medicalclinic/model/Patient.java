package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.UpdatePatientCommand;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class Patient {
    private String email;
    private String password;
    private String idCardNo;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate birthday;

    public String getPassword() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void update(UpdatePatientCommand command) {
        setEmail(command.email());
        setPassword(command.password());
        setIdCardNo(command.idCardNo());
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
}
