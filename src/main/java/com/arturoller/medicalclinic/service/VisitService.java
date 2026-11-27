package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.CreateVisitCommand;
import com.arturoller.medicalclinic.dto.PageDto;
import com.arturoller.medicalclinic.dto.SearchVisitCommand;
import com.arturoller.medicalclinic.dto.VisitDto;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.VisitMapper;
import com.arturoller.medicalclinic.model.Doctor;
import com.arturoller.medicalclinic.model.Institution;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.Visit;
import com.arturoller.medicalclinic.repository.DoctorRepository;
import com.arturoller.medicalclinic.repository.InstitutionRepository;
import com.arturoller.medicalclinic.repository.PatientRepository;
import com.arturoller.medicalclinic.repository.VisitRepository;
import com.arturoller.medicalclinic.util.SortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisitService {
    private static final int VISIT_GRANULARITY_MINUTES = 15;
    private static final Duration DEFAULT_SEARCH_RANGE = Duration.ofDays(30);

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final InstitutionRepository institutionRepository;
    private final VisitMapper visitMapper;
    private final Clock clock;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "startTime", "endTime");
    private final SortValidator sortValidator;

    @Transactional(readOnly = true)
    public PageDto<VisitDto> search(SearchVisitCommand query, Pageable pageable) {
        sortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);
        LocalDateTime from = query.from() != null ? query.from() : LocalDateTime.now(clock);
        LocalDateTime to = query.to() != null ? query.to() : from.plus(DEFAULT_SEARCH_RANGE);
        Page<VisitDto> content = visitRepository.search(query.doctorId(), query.institutionId(), from, to, pageable)
                .map(visitMapper::toDto);
        return PageDto.from(content);
    }

    @Transactional(readOnly = true)
    public VisitDto getById(Long visitId) {
        return visitMapper.toDto(findVisitById(visitId));
    }

    @Transactional
    public VisitDto create(CreateVisitCommand command) {
        validateTimes(command.startTime(), command.endTime());
        Doctor doctor = findDoctorById(command.doctorId());
        Institution institution = findInstitutionById(command.institutionId());
        if (!doctor.hasInstitution(institution)) {
            throw new InstitutionNotAssignedException("Placówka " + institution.getId()
                    + " nie jest przypisana do lekarza " + doctor.getId());
        }
        if (visitRepository.doctorHasOverlappingVisit(command.doctorId(), command.startTime(), command.endTime())) {
            throw new VisitOverlapException("Lekarz " + doctor.getId() + " ma już termin w przedziale "
                    + command.startTime() + " – " + command.endTime());
        }
        Visit visit = Visit.create(doctor, institution, command.startTime(), command.endTime());
        return visitMapper.toDto(visitRepository.save(visit));
    }

    @Transactional
    public void delete(Long visitId) {
        Visit visit = findVisitById(visitId);
        if (visit.getPatient() != null) {
            throw new PatientAlreadyAssignedException("Termin " + visitId + " jest zarezerwowany przez pacjenta " + visit.getPatient().getId());
        }
        visitRepository.delete(visit);
    }

    @Transactional
    public void bookPatient(Long visitId, Long patientId) {
        Visit visit = findVisitById(visitId);
        Patient patient = findPatientById(patientId);
        if (!visit.isAvailable()) {
            throw new VisitAlreadyBookedException("Termin " + visitId + " jest już zarezerwowany");
        }
        if (!visit.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new VisitAlreadyStartedException("Termin " + visitId + " już się rozpoczął");
        }
        if (visitRepository.patientHasOverlappingVisit(patient.getId(), visit.getStartTime(), visit.getEndTime())) {
            throw new VisitOverlapException("Pacjent " + patient.getId() + " ma w tym czasie inną wizytę");
        }
        visit.book(patient);
        visitRepository.save(visit);
    }

    @Transactional
    public void cancelPatient(Long visitId, Long patientId) {
        Visit visit = findVisitById(visitId);
        if (visit.isAvailable() || !visit.getPatient().getId().equals(patientId)) {
            throw new VisitNotBookedByPatientException("Termin " + visitId + " nie jest zarezerwowany przez pacjenta " + patientId);
        }
        if (!visit.getStartTime().isAfter(LocalDateTime.now(clock))) {
            throw new VisitAlreadyStartedException("Termin " + visitId + " już się rozpoczął");
        }
        visit.cancel();
        visitRepository.save(visit);
    }

    private void validateTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new InvalidVisitTimeException("Początek terminu musi być przed jego końcem");
        }
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            throw new InvalidVisitTimeException("Termin musi zaczynać się w przyszłości");
        }
        if (!isFullQuarter(startTime) || !isFullQuarter(endTime)) {
            throw new InvalidVisitTimeException("Termin musi zaczynać się i kończyć o :00, :15, :30 lub :45");
        }
    }

    private boolean isFullQuarter(LocalDateTime time) {
        return time.getMinute() % VISIT_GRANULARITY_MINUTES == 0
                && time.getSecond() == 0
                && time.getNano() == 0;
    }

    private Patient findPatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Nie znaleziono pacjenta: " + patientId));
    }

    private Visit findVisitById(Long visitId) {
        return visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException("Nie znaleziono terminu: " + visitId));
    }

    private Doctor findDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException("Nie znaleziono lekarza: " + doctorId));
    }

    private Institution findInstitutionById(Long institutionId) {
        return institutionRepository.findById(institutionId)
                .orElseThrow(() -> new InstitutionNotFoundException("Nie znaleziono placówki: " + institutionId));
    }
}
