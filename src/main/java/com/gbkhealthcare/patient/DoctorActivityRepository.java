package com.gbkhealthcare.patient;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DoctorActivityRepository extends JpaRepository<DoctorActivity, Long> {
    List<DoctorActivity> findByDoctor_Id(Long doctorId, Pageable pageable);
}
