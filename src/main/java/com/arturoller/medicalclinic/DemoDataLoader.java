package com.arturoller.medicalclinic;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.dto.CreateUserCommand;
import com.arturoller.medicalclinic.dto.UserDto;
import com.arturoller.medicalclinic.model.Email;
import com.arturoller.medicalclinic.service.PatientService;
import com.arturoller.medicalclinic.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@AllArgsConstructor
public class DemoDataLoader implements CommandLineRunner {
    private final UserService userService;
    private final PatientService patientService;

    @Override
    public void run(String... args) {
        // users with an assigned patient
        UserDto jan = createUser("jan.kowalski@example.com", "ABC123456");
        createPatient("Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12), jan.id());

        UserDto anna = createUser("anna.nowak@example.com", "DEF234567");
        createPatient("Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24), anna.id());

        UserDto piotr = createUser("piotr.wisniewski@example.com", "GHI345678");
        createPatient("Piotr", "Wiśniewski", "502300400", LocalDate.of(1978, 11, 5), piotr.id());

        // users without a patient
        createUser("katarzyna.wojcik@example.com", "JKL456789");
        createUser("tomasz.kaminski@example.com", "MNO567890");

        // patient without a user
//        createPatient("Magdalena", "Lewandowska", "505600700", LocalDate.of(1995, 5, 8), null);
    }

    private UserDto createUser(String email, String idCardNo) {
        return userService.create(new CreateUserCommand(new Email(email), "haslo123", idCardNo));
    }

    private void createPatient(String firstName, String lastName, String phoneNumber, LocalDate birthday, Integer userId) {
        patientService.create(new CreatePatientCommand(firstName, lastName, phoneNumber, birthday, userId));
    }
}