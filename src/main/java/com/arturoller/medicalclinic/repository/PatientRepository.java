package com.arturoller.medicalclinic.repository;

import com.arturoller.medicalclinic.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByUserId(Long userId);

    Page<Patient> findAllByDoctors_Id(Long doctorId, Pageable pageable);
}
