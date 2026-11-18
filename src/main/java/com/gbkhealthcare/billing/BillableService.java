package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.time.LocalDateTime;
@Entity @Table(name="billable_services")
public class BillableService {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=40) private String code;
 @Column(nullable=false,length=160) private String name;
 @Column(columnDefinition="TEXT") private String description;
 @Column(nullable=false,length=24) private String category;
 @JdbcTypeCode(Types.BIT) @Column(nullable=false) private boolean active=true;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 protected BillableService(){}
 public BillableService(String code,String name,String description,String category,LocalDateTime at){this.code=code.trim().toUpperCase();this.name=name.trim();this.description=clean(description);this.category=category.trim().toUpperCase();this.createdAt=at;}
 public void setActive(boolean value){active=value;} private static String clean(String s){return s==null||s.isBlank()?null:s.trim();}
 public void updateDetails(String code,String name,String description,String category){this.code=code.trim().toUpperCase();this.name=name.trim();this.description=clean(description);this.category=category.trim().toUpperCase();}
 public Long getId(){return id;} public String getCode(){return code;} public String getName(){return name;} public String getDescription(){return description;} public String getCategory(){return category;} public boolean isActive(){return active;} public LocalDateTime getCreatedAt(){return createdAt;}
}
