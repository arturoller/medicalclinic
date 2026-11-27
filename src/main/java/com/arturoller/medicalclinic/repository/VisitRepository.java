package com.arturoller.medicalclinic.repository;

import com.arturoller.medicalclinic.model.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    @Query("""
        SELECT COUNT(v) > 0 FROM Visit v
            WHERE v.doctor.id = :doctorId
                AND v.startTime < :endTime
                AND v.endTime > :startTime
        """)
    boolean doctorHasOverlappingVisit(
            @Param("doctorId") Long doctorId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT v FROM Visit v
            WHERE (:doctorId IS NULL OR v.doctor.id = :doctorId)
                AND (:institutionId IS NULL OR v.institution.id = :institutionId)
                AND v.startTime >= :startTime
                AND v.startTime < :endTime
        """)
    Page<Visit> search(
            @Param("doctorId") Long doctorId,
            @Param("institutionId") Long institutionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(v) > 0 FROM Visit v
            WHERE v.patient.id = :patientId
                AND v.startTime < :endTime
                AND v.endTime > :startTime
        """)
    boolean patientHasOverlappingVisit(
            @Param("patientId") Long patientId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    boolean existsByDoctorId(Long doctorId);

    boolean existsByInstitutionId(Long institutionId);

    boolean existsByPatientId(Long patientId);
}
