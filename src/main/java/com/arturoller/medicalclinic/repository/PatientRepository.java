package com.arturoller.medicalclinic.repository;

import com.arturoller.medicalclinic.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Integer> {
    boolean existsByUserId(Integer userId);
}
