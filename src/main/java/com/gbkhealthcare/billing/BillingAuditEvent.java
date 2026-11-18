package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="billing_audit_events",indexes=@Index(name="idx_billing_audit_invoice_time",columnList="invoice_id, occurred_at"))
public class BillingAuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="invoice_id") private Long invoiceId;
 @Column(name="actor_role",nullable=false,length=16,updatable=false) private String actorRole;
 @Column(name="actor_id",nullable=false,updatable=false) private Long actorId;
 @Column(name="actor_email",nullable=false,length=160,updatable=false) private String actorEmail;
 @Column(name="action_type",nullable=false,length=40,updatable=false) private String actionType;
 @Column(name="resource_type",nullable=false,length=24,updatable=false) private String resourceType;
 @Column(name="resource_id",nullable=false,updatable=false) private Long resourceId;
 @Column(name="occurred_at",nullable=false,updatable=false) private LocalDateTime occurredAt;
 protected BillingAuditEvent(){}
 public BillingAuditEvent(Long invoiceId,String role,Long actor,String email,String action,String type,Long resource,LocalDateTime at){this.invoiceId=invoiceId;actorRole=role;actorId=actor;actorEmail=email;actionType=action;resourceType=type;resourceId=resource;occurredAt=at;}
 public Long getId(){return id;} public Long getInvoiceId(){return invoiceId;} public String getActorRole(){return actorRole;} public Long getActorId(){return actorId;} public String getActorEmail(){return actorEmail;} public String getActionType(){return actionType;} public String getResourceType(){return resourceType;} public Long getResourceId(){return resourceId;} public LocalDateTime getOccurredAt(){return occurredAt;}
}
