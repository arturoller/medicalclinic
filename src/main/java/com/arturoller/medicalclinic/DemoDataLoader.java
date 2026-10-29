package com.arturoller.medicalclinic;

import com.arturoller.medicalclinic.dto.PatientCreateRequest;
import com.arturoller.medicalclinic.model.PatientModel;
import com.arturoller.medicalclinic.service.PatientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DemoDataLoader implements CommandLineRunner {

    private final PatientService patientService;

    public DemoDataLoader(PatientService patientService) {
        this.patientService = patientService;
    }

    @Override
    public void run(String... args) {
        patientService.addPatient(new PatientModel("jan.kowalski@example.com", "haslo123", "ABC123456",
                "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12)));
        patientService.addPatient(new PatientModel("anna.nowak@example.com", "haslo123", "DEF234567",
                "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24)));
        patientService.addPatient(new PatientModel("piotr.wisniewski@example.com", "haslo123", "GHI345678",
                "Piotr", "Wiśniewski", "502300400", LocalDate.of(1978, 11, 5)));
        patientService.addPatient(new PatientModel("katarzyna.wojcik@example.com", "haslo123", "JKL456789",
                "Katarzyna", "Wójcik", "503400500", LocalDate.of(2001, 1, 30)));
        patientService.addPatient(new PatientModel("tomasz.kaminski@example.com", "haslo123", "MNO567890",
                "Tomasz", "Kamiński", "504500600", LocalDate.of(1968, 9, 18)));
    }

}