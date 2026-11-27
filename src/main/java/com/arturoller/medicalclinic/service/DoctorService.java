package com.arturoller.medicalclinic.service;

import com.arturoller.medicalclinic.dto.*;
import com.arturoller.medicalclinic.exception.*;
import com.arturoller.medicalclinic.mapper.DoctorMapper;
import com.arturoller.medicalclinic.mapper.InstitutionMapper;
import com.arturoller.medicalclinic.mapper.PatientMapper;
import com.arturoller.medicalclinic.model.Doctor;
import com.arturoller.medicalclinic.model.Institution;
import com.arturoller.medicalclinic.model.Patient;
import com.arturoller.medicalclinic.model.User;
import com.arturoller.medicalclinic.repository.*;
import com.arturoller.medicalclinic.util.SortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DoctorService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final InstitutionRepository institutionRepository;
    private final VisitRepository visitRepository;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final InstitutionMapper institutionMapper;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "firstName", "lastName");
    private static final Set<String> PATIENT_SORT_FIELDS = Set.of("id", "firstName", "lastName", "birthday");
    private static final Set<String> INSTITUTION_SORT_FIELDS = Set.of("id", "name", "city");
    private final SortValidator sortValidator;

    @Transactional(readOnly = true)
    public PageDto<DoctorDto> list(Pageable pageable) {
        sortValidator.validate(pageable.getSort(), ALLOWED_SORT_FIELDS);
        Page<DoctorDto> content = doctorRepository.findAll(pageable)
                .map(doctorMapper::toDto);
        return PageDto.from(content);
    }

    @Transactional(readOnly = true)
    public DoctorDto getById(Long doctorId) {
        return doctorMapper.toDto(findDoctorById(doctorId));
    }

    @Transactional
    public DoctorDto save(CreateDoctorCommand command) {
        User user = findUserById(command.userId());
        if (doctorRepository.existsByUserId(command.userId())) {
            throw new UserAlreadyAssignedException("Użytkownik " + command.userId() + " jest już przypisany do innego lekarza");
        }
        Doctor doctor = doctorMapper.toModel(command);
        doctor.setUser(user);
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    @Transactional
    public DoctorDto update(Long doctorId, UpdateDoctorCommand command) {
        Doctor doctor = findDoctorById(doctorId);
        doctor.update(command);
        return doctorMapper.toDto(doctorRepository.save(doctor));
    }

    @Transactional
    public void delete(Long doctorId) {
        Doctor doctor = findDoctorById(doctorId);
        if (!doctor.getPatients().isEmpty()) {
            throw new DoctorAlreadyAssignedException("Lekarz " + doctorId + " ma przypisanych pacjentów");
        }
        if (!doctor.getInstitutions().isEmpty()) {
            throw new DoctorAlreadyAssignedException("Lekarz " + doctorId + " jest przypisany do placówek");
        }
        if (visitRepository.existsByDoctorId(doctorId)) {
            throw new DoctorAlreadyAssignedException("Lekarz " + doctorId + " ma wizyty");
        }
        doctorRepository.delete(doctor);
    }

    @Transactional
    public void assignPatient(Long doctorId, Long patientId) {
        Doctor doctor = findDoctorById(doctorId);
        Patient patient = findPatientById(patientId);
        if (doctor.hasPatient(patient)) {
            throw new PatientAlreadyAssignedException("Pacjent " + patientId + " jest już przypisany do lekarza " + doctorId);
        }
        doctor.assignPatient(patient);
        doctorRepository.save(doctor);
    }

    @Transactional
    public void unassignPatient(Long doctorId, Long patientId) {
        Doctor doctor = findDoctorById(doctorId);
        Patient patient = findPatientById(patientId);
        if (!doctor.hasPatient(patient)) {
            throw new PatientNotAssignedException("Pacjent " + patientId + " nie jest przypisany do lekarza " + doctorId);
        }
        doctor.unassignPatient(patient);
        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public PageDto<PatientDto> getPatients(Long doctorId, Pageable pageable) {
        sortValidator.validate(pageable.getSort(), PATIENT_SORT_FIELDS);
        ensureDoctorExists(doctorId);
        Page<PatientDto> content = patientRepository.findAllByDoctors_Id(doctorId, pageable)
                .map(patientMapper::toDto);
        return PageDto.from(content);
    }

    @Transactional
    public void assignInstitution(Long doctorId, Long institutionId) {
        Doctor doctor = findDoctorById(doctorId);
        Institution institution = findInstitutionById(institutionId);
        if (doctor.hasInstitution(institution)) {
            throw new InstitutionAlreadyAssignedException("Placówka " + institutionId + " jest już przypisana do lekarza " + doctorId);
        }
        doctor.assignInstitution(institution);
        doctorRepository.save(doctor);
    }

    @Transactional
    public void unassignInstitution(Long doctorId, Long institutionId) {
        Doctor doctor = findDoctorById(doctorId);
        Institution institution = findInstitutionById(institutionId);
        if (!doctor.hasInstitution(institution)) {
            throw new InstitutionNotAssignedException("Placówka " + institutionId + " nie jest przypisana do lekarza " + doctorId);
        }
        doctor.unassignInstitution(institution);
        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public PageDto<InstitutionDto> getInstitutions(Long doctorId, Pageable pageable) {
        sortValidator.validate(pageable.getSort(), INSTITUTION_SORT_FIELDS);
        ensureDoctorExists(doctorId);
        Page<InstitutionDto> content = institutionRepository.findAllByDoctors_Id(doctorId, pageable)
                .map(institutionMapper::toDto);
        return PageDto.from(content);
    }

    private void ensureDoctorExists(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new DoctorNotFoundException("Nie znaleziono lekarza: " + doctorId);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Nie znaleziono użytkownika: " + userId));
    }

    private Patient findPatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Nie znaleziono pacjenta: " + patientId));
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
