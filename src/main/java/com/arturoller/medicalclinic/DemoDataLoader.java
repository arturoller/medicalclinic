package com.arturoller.medicalclinic;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.service.*;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class DemoDataLoader implements CommandLineRunner {
    private final UserService userService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final InstitutionService institutionService;
    private final VisitService visitService;
    private final Clock clock;

    @Override
    public void run(String... args) {
        // patients (each with own user)
        PatientDto jan = createPatient("jan.kowalski@example.com", "Jan", "Kowalski", "500100200", LocalDate.of(1985, 3, 12));
        PatientDto anna = createPatient("anna.nowak@example.com", "Anna", "Nowak", "501200300", LocalDate.of(1992, 7, 24));
        PatientDto piotr = createPatient("piotr.wisniewski@example.com", "Piotr", "Wiśniewski", "502300400", LocalDate.of(1978, 11, 5));

        // users without a patient or doctor
        createUser("katarzyna.wojcik@example.com");
        createUser("tomasz.kaminski@example.com");

        PatientDto magdalena = createPatient("magdalena.kaczmarek@example.com", "Magdalena", "Kaczmarek", "503400500", LocalDate.of(2001, 1, 30));
        createPatient("pawel.mazur@example.com", "Paweł", "Mazur", "504500600", LocalDate.of(1969, 9, 17)); // no doctors

        // doctors (each with own user)
        DoctorDto ewa = createDoctor("ewa.zielinska@example.com", "Ewa", "Zielińska", "600100200");
        DoctorDto marek = createDoctor("marek.lewandowski@example.com", "Marek", "Lewandowski", "601200300");
        createDoctor("agnieszka.dabrowska@example.com", "Agnieszka", "Dąbrowska", "602300400"); // no patients, no institutions

        // institutions
        InstitutionDto zdrowie = createInstitution("Przychodnia Zdrowie", "Warszawa", "00-950", "Marszałkowska", "10");
        InstitutionDto vita = createInstitution("Centrum Medyczne Vita", "Kraków", "31-010", "Długa", "5");
        createInstitution("Szpital Miejski", "Gdańsk", "80-210", "Grunwaldzka", "100"); // no doctors

        // doctor <-> institution
        doctorService.assignInstitution(ewa.id(), zdrowie.id());
        doctorService.assignInstitution(ewa.id(), vita.id());
        doctorService.assignInstitution(marek.id(), vita.id());

        // doctor <-> patient
        doctorService.assignPatient(ewa.id(), jan.id());
        doctorService.assignPatient(ewa.id(), anna.id());
        doctorService.assignPatient(marek.id(), anna.id());
        doctorService.assignPatient(marek.id(), piotr.id());
        doctorService.assignPatient(marek.id(), magdalena.id());

        // visits – relative to today (validation rejects past dates)
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfterTomorrow = today.plusDays(2);
        LocalDate nextWeek = today.plusWeeks(1);
        LocalDate in40Days = today.plusDays(40);

        // Ewa @ Zdrowie – adjacent 15-min visits + one 30-min
        VisitDto ewaTomorrow0800 = createVisit(ewa, zdrowie, tomorrow.atTime(8, 0), tomorrow.atTime(8, 15));
        VisitDto ewaTomorrow0815 = createVisit(ewa, zdrowie, tomorrow.atTime(8, 15), tomorrow.atTime(8, 30));
        createVisit(ewa, zdrowie, tomorrow.atTime(8, 30), tomorrow.atTime(8, 45));
        createVisit(ewa, zdrowie, tomorrow.atTime(9, 0), tomorrow.atTime(9, 30));               // gap 08:45–09:00

        // Ewa @ Vita – second institution, another day
        createVisit(ewa, vita, dayAfterTomorrow.atTime(10, 0), dayAfterTomorrow.atTime(10, 15));
        createVisit(ewa, vita, dayAfterTomorrow.atTime(10, 15), dayAfterTomorrow.atTime(10, 30));

        // Marek @ Vita – same hour as Ewa (different doctor = OK), one 45-min
        VisitDto marekTomorrow0800 = createVisit(marek, vita, tomorrow.atTime(8, 0), tomorrow.atTime(8, 15));
        createVisit(marek, vita, tomorrow.atTime(8, 15), tomorrow.atTime(9, 0));
        VisitDto marekNextWeek = createVisit(marek, vita, nextWeek.atTime(12, 0), nextWeek.atTime(12, 30));

        // outside the default 30-day search range
        createVisit(ewa, zdrowie, in40Days.atTime(11, 0), in40Days.atTime(11, 15));

        // bookings – only patients assigned to these doctors
        bookVisit(ewaTomorrow0800, jan);
        bookVisit(ewaTomorrow0815, anna);
        bookVisit(marekTomorrow0800, piotr);
        bookVisit(marekNextWeek, magdalena);
    }

    private UserDto createUser(String email) {
        return userService.create(new CreateUserCommand(email, "haslo123"));
    }

    private PatientDto createPatient(String email, String firstName, String lastName, String phoneNumber, LocalDate birthday) {
        UserDto user = createUser(email);
        return patientService.create(new CreatePatientCommand(user.id(), firstName, lastName, phoneNumber, birthday));
    }

    private DoctorDto createDoctor(String email, String firstName, String lastName, String phoneNumber) {
        UserDto user = createUser(email);
        return doctorService.save(new CreateDoctorCommand(user.id(), firstName, lastName, phoneNumber));
    }

    private InstitutionDto createInstitution(String name, String city, String postCode, String street, String buildingNumber) {
        return institutionService.save(new CreateInstitutionCommand(name, city, postCode, street, buildingNumber));
    }

    private VisitDto createVisit(DoctorDto doctor, InstitutionDto institution, LocalDateTime start, LocalDateTime end) {
        return visitService.create(new CreateVisitCommand(doctor.id(), institution.id(), start, end));
    }
    private void bookVisit(VisitDto visit, PatientDto patient) {
        visitService.bookPatient(visit.id(), patient.id());
    }
}