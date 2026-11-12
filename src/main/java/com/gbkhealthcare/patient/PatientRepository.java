
package com.gbkhealthcare.patient;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PatientRepository
                extends JpaRepository<Patient, Long> {

        long countByDoctorAccount_Id(Long doctorAccountId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Patient p WHERE p.id = :patientId")
        Optional<Patient> findByIdForUpdate(
                        @Param("patientId") Long patientId);

        List<Patient> findAllByDoctorAccount_IdOrderByVisitDateDesc(
                        Long doctorAccountId);

        @Query(value = """
                        SELECT p.*
                        FROM patients p
                        LEFT JOIN (
                            SELECT v.patient_id, MIN(v.created_at) AS registered_at
                            FROM patient_visits v
                            JOIN patients scoped ON scoped.id = v.patient_id
                            WHERE scoped.doctor_account_id = :doctorAccountId
                              AND v.visit_number = 1
                            GROUP BY v.patient_id
                        ) initial_visit ON initial_visit.patient_id = p.id
                        WHERE p.doctor_account_id = :doctorAccountId
                          AND (:gender IS NULL OR p.gender = :gender)
                          AND (:search IS NULL
                               OR LOWER(p.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
                        ORDER BY
                            CASE WHEN initial_visit.registered_at IS NULL THEN 1 ELSE 0 END ASC,
                            initial_visit.registered_at DESC,
                            p.id DESC
                        """,
                        countQuery = """
                        SELECT COUNT(*)
                        FROM patients p
                        WHERE p.doctor_account_id = :doctorAccountId
                          AND (:gender IS NULL OR p.gender = :gender)
                          AND (:search IS NULL
                               OR LOWER(p.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
                        """,
                        nativeQuery = true)
        Page<Patient> findPageByDoctorAndFilters(
                        @Param("doctorAccountId") Long doctorAccountId,
                        @Param("search") String search,
                        @Param("gender") String gender,
                        Pageable pageable);

        @Query(value = """
                        SELECT p.id AS id, p.full_name AS fullName, p.age AS age,
                               p.gender AS gender, p.visit_date AS visitDate,
                               p.phone AS phone, p.email AS email, p.doctor AS doctor,
                               p.chief_complaints AS chiefComplaints
                        FROM patients p
                        WHERE p.doctor_account_id = :doctorAccountId
                          AND p.id > :afterId
                          AND (:gender IS NULL OR p.gender = :gender)
                          AND (:search IS NULL
                               OR LOWER(p.full_name) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(p.phone) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(COALESCE(p.email, '')) LIKE LOWER(CONCAT('%', :search, '%')))
                        ORDER BY p.id ASC
                        LIMIT 500
                        """, nativeQuery = true)
        List<PatientCsvRow> findCsvExportBatch(
                        @Param("doctorAccountId") Long doctorAccountId,
                        @Param("afterId") Long afterId,
                        @Param("search") String search,
                        @Param("gender") String gender);

        Optional<Patient> findByIdAndDoctorAccount_Id(
                        Long patientId,
                        Long doctorAccountId);
}
