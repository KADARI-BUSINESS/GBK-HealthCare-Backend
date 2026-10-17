package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false, length = 10)
    private String phone;

    @Column(length = 160)
    private String email;

    private Double temperature;
    private Integer pulseRate;
    private Integer spo2;
    private Integer bpSystolic;
    private Integer bpDiastolic;

    @Column(columnDefinition = "TEXT")
    private String chiefComplaints;

    @Column(columnDefinition = "TEXT")
    private String presentIllness;

    @Column(columnDefinition = "TEXT")
    private String generalExamination;

    @Column(columnDefinition = "TEXT")
    private String heartExamination;

    @Column(columnDefinition = "TEXT")
    private String lungsExamination;

    @Column(columnDefinition = "TEXT")
    private String abdomenExamination;

    @Column(columnDefinition = "TEXT")
    private String rectalExamination;

    @Column(nullable = false, length = 100)
    private String doctor;

    public Patient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getPulseRate() { return pulseRate; }
    public void setPulseRate(Integer pulseRate) { this.pulseRate = pulseRate; }

    public Integer getSpo2() { return spo2; }
    public void setSpo2(Integer spo2) { this.spo2 = spo2; }

    public Integer getBpSystolic() { return bpSystolic; }
    public void setBpSystolic(Integer bpSystolic) { this.bpSystolic = bpSystolic; }

    public Integer getBpDiastolic() { return bpDiastolic; }
    public void setBpDiastolic(Integer bpDiastolic) { this.bpDiastolic = bpDiastolic; }

    public String getChiefComplaints() { return chiefComplaints; }
    public void setChiefComplaints(String chiefComplaints) { this.chiefComplaints = chiefComplaints; }

    public String getPresentIllness() { return presentIllness; }
    public void setPresentIllness(String presentIllness) { this.presentIllness = presentIllness; }

    public String getGeneralExamination() { return generalExamination; }
    public void setGeneralExamination(String generalExamination) { this.generalExamination = generalExamination; }

    public String getHeartExamination() { return heartExamination; }
    public void setHeartExamination(String heartExamination) { this.heartExamination = heartExamination; }

    public String getLungsExamination() { return lungsExamination; }
    public void setLungsExamination(String lungsExamination) { this.lungsExamination = lungsExamination; }

    public String getAbdomenExamination() { return abdomenExamination; }
    public void setAbdomenExamination(String abdomenExamination) { this.abdomenExamination = abdomenExamination; }

    public String getRectalExamination() { return rectalExamination; }
    public void setRectalExamination(String rectalExamination) { this.rectalExamination = rectalExamination; }

    public String getDoctor() { return doctor; }
    public void setDoctor(String doctor) { this.doctor = doctor; }
}
