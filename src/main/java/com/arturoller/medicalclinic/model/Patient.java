package com.arturoller.medicalclinic.model;

import com.arturoller.medicalclinic.dto.ChangePasswordPatientCommand;
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

    // Nie pozwola na zmiane dowodu osobistego uzytkownika
    public void setIdCardNo(String idCardNo) {
        throw new UnsupportedOperationException("You can't change id card no");
    }

    // Nie pozwala na odczytanie hasła! Cała logika z operacjami na haśle powinna znajdować się tutaj!
    public String getPassword() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Nie pozwala na ustawienie hasłą z zewnątrz!
    public String setPassword(String password) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean passwordEquals(String password) {
        return password.equals(this.password);
    }

    public void changePassword(ChangePasswordPatientCommand command) {
        password = command.password();
    }
}
