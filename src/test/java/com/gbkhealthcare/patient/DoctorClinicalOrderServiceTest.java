package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DoctorClinicalOrderServiceTest {
    DoctorRepository doctors; DoctorQueueEntryRepository queue; PrescriptionRepository prescriptions;
    InvestigationOrderRepository investigations; ReferralOrderRepository referrals; ClinicalOrderAuditEventRepository audit;
    DoctorClinicalOrderService service; Doctor doctor; DoctorQueueEntry assigned; PatientVisit visit; Jwt jwt;
    @BeforeEach void setup(){
        doctors=mock(DoctorRepository.class);queue=mock(DoctorQueueEntryRepository.class);prescriptions=mock(PrescriptionRepository.class);investigations=mock(InvestigationOrderRepository.class);referrals=mock(ReferralOrderRepository.class);audit=mock(ClinicalOrderAuditEventRepository.class);
        service=new DoctorClinicalOrderService(doctors,queue,prescriptions,investigations,referrals,audit);
        doctor=new Doctor();doctor.setId(9L);doctor.setEmail("doc@example.test");doctor.setEnabled(true);doctor.setApprovalStatus(Doctor.ApprovalStatus.APPROVED);
        visit=mock(PatientVisit.class);when(visit.getId()).thenReturn(44L);assigned=mock(DoctorQueueEntry.class);when(assigned.getPatientVisit()).thenReturn(visit);when(assigned.getStatus()).thenReturn("IN_CONSULTATION");
        when(doctors.findById(9L)).thenReturn(Optional.of(doctor));when(queue.findAssignedForUpdate(3L,9L)).thenReturn(Optional.of(assigned));when(queue.findAssigned(3L,9L)).thenReturn(Optional.of(assigned));
        jwt=Jwt.withTokenValue("opaque").header("alg","none").subject("doc@example.test").claim("doctorId",9L).build();
    }
    private PrescriptionRequest rx(){return new PrescriptionRequest("Take with food",List.of(new PrescriptionItemRequest("Medicine A","500 mg","Oral","Twice daily","5 days",new BigDecimal("10"),"After meals",LocalDate.now()),new PrescriptionItemRequest("Medicine B","10 mg","Oral","Once daily","3 days",new BigDecimal("3"),null,LocalDate.now())),null);}
    @Test void createsMultiItemDraftWithStableZeroBasedOrderAndAudits(){when(prescriptions.countByPatientVisit_Id(44L)).thenReturn(0);when(prescriptions.save(any())).thenAnswer(i->i.getArgument(0));PrescriptionResponse result=service.createPrescription(jwt,3L,rx());assertEquals(1,result.revision());assertEquals("DRAFT",result.status().name());assertEquals(2,result.items().size());assertEquals(List.of("Medicine A","Medicine B"),result.items().stream().map(PrescriptionItemResponse::medicineName).toList());ArgumentCaptor<Prescription> saved=ArgumentCaptor.forClass(Prescription.class);verify(prescriptions).save(saved.capture());assertEquals(List.of(0,1),saved.getValue().getItems().stream().map(PrescriptionItem::getItemOrder).toList());verify(audit).save(any(ClinicalOrderAuditEvent.class));}
    @Test void createsSingleItemDraftAtOrderZero(){when(prescriptions.countByPatientVisit_Id(44L)).thenReturn(0);when(prescriptions.save(any())).thenAnswer(i->i.getArgument(0));PrescriptionRequest single=new PrescriptionRequest(null,List.of(rx().items().get(0)),null);PrescriptionResponse result=service.createPrescription(jwt,3L,single);assertEquals(1,result.items().size());ArgumentCaptor<Prescription> saved=ArgumentCaptor.forClass(Prescription.class);verify(prescriptions).save(saved.capture());assertEquals(0,saved.getValue().getItems().get(0).getItemOrder());}
    @Test void deniesDoctorWhoseEncounterIsNotAssigned(){when(queue.findAssignedForUpdate(3L,9L)).thenReturn(Optional.empty());assertThrows(ResponseStatusException.class,()->service.createPrescription(jwt,3L,rx()));verifyNoInteractions(prescriptions);}
    @Test void signedPrescriptionIsImmutable(){Prescription signed=new Prescription(visit,doctor,1,null,"note",LocalDateTime.now());signed.replaceItems(rx().items().stream().map(PrescriptionItem::new).toList());signed.sign(LocalDateTime.now());when(prescriptions.findById(8L)).thenReturn(Optional.of(signed));assertThrows(ResponseStatusException.class,()->service.updatePrescription(jwt,3L,8L,rx()));verify(prescriptions,never()).save(any());}
    @Test void signingDraftMakesItImmutableAndAudits(){Prescription draft=new Prescription(visit,doctor,1,null,"note",LocalDateTime.now());draft.replaceItems(rx().items().stream().map(PrescriptionItem::new).toList());when(prescriptions.findById(8L)).thenReturn(Optional.of(draft));when(prescriptions.save(any())).thenAnswer(i->i.getArgument(0));PrescriptionResponse result=service.signPrescription(jwt,3L,8L);assertEquals(Prescription.State.SIGNED,result.status());assertNotNull(result.signedAt());verify(audit).save(argThat(e->e.getActionType().equals("PRESCRIPTION_SIGNED")));assertThrows(IllegalStateException.class,()->draft.updateNotes("change"));}
    @Test void createsInvestigationWithoutAssumingResultAndAudits(){when(investigations.save(any())).thenAnswer(i->i.getArgument(0));InvestigationOrderResponse result=service.createInvestigation(jwt,3L,new InvestigationOrderRequest(InvestigationOrder.Type.LAB,"CBC","Fever",InvestigationOrder.Priority.URGENT));assertEquals(InvestigationOrder.Status.ORDERED,result.status());verify(audit).save(any(ClinicalOrderAuditEvent.class));}
    @Test void createsReferralAndAudits(){when(referrals.save(any())).thenAnswer(i->i.getArgument(0));ReferralOrderResponse result=service.createReferral(jwt,3L,new ReferralOrderRequest("Cardiology",null,"Chest discomfort",ReferralOrder.Priority.URGENT,"Review within 24 hours"));assertEquals(ReferralOrder.Status.CREATED,result.status());verify(audit).save(any(ClinicalOrderAuditEvent.class));}
}
