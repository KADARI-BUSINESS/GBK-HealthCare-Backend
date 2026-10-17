
package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientVisitRepository
        extends JpaRepository<PatientVisit, Long> {

    List<PatientVisit> findByPatient_IdOrderByVisitDateDesc(Long patientId);

    @Query("""
            SELECT COALESCE(MAX(v.visitNumber), 0)
            FROM PatientVisit v
            WHERE v.patient.id = :patientId
            """)
    Integer findMaxVisitNumberByPatientId(
            @Param("patientId") Long patientId);
}
