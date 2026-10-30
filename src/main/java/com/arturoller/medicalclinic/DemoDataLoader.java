package com.arturoller.medicalclinic;

import com.arturoller.medicalclinic.dto.CreatePatientCommand;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.service.PatientService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@AllArgsConstructor
public class DemoDataLoader implements CommandLineRunner {
    private final PatientService patientService;

    @Override
    public void run(String... args) {
        patientService.create(new CreatePatientCommand("jan.kowalski@example.com", "haslo123", "ABC123456",
                "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)));
        patientService.create(new CreatePatientCommand("anna.nowak@example.com", "haslo123", "DEF234567",
                "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24)));
        patientService.create(new CreatePatientCommand("piotr.wisniewski@example.com", "haslo123", "GHI345678",
                "Piotr", "Wiśniewski", "502300400", LocalDate.of(1978, 11, 5)));
        patientService.create(new CreatePatientCommand("katarzyna.wojcik@example.com", "haslo123", "JKL456789",
                "Katarzyna", "Wójcik", "503400500", LocalDate.of(2001, 1, 30)));
        patientService.create(new CreatePatientCommand("tomasz.kaminski@example.com", "haslo123", "MNO567890",
                "Tomasz", "Kamiński", "504500600", LocalDate.of(1968, 9, 18)));
    }
}