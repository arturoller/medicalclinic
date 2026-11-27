package com.arturoller.medicalclinic.repository;

import com.arturoller.medicalclinic.model.Institution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstitutionRepository extends JpaRepository<Institution, Long> {
    boolean existsByNameIgnoreCase(String name);

    Page<Institution> findAllByDoctors_Id(Long doctorId, Pageable pageable);
}
