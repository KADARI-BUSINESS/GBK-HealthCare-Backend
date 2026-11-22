package com.gbkhealthcare.patient;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "prescription_items", uniqueConstraints = @UniqueConstraint(name = "uk_prescription_item_order", columnNames = {"prescription_id", "item_order"}))
public class PrescriptionItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "prescription_id", nullable = false) private Prescription prescription;
    @Column(name = "item_order", nullable = false) private int itemOrder;
    @Column(name = "medicine_name", nullable = false, length = 180) private String medicineName;
    @Column(nullable = false, length = 120) private String dosage;
    @Column(nullable = false, length = 80) private String route;
    @Column(nullable = false, length = 120) private String frequency;
    @Column(nullable = false, length = 80) private String duration;
    @Column(nullable = false, precision = 7, scale = 2) private BigDecimal quantity;
    @Column(columnDefinition = "TEXT") private String instructions;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    protected PrescriptionItem() {}
    public PrescriptionItem(PrescriptionItemRequest request) {
        medicineName = request.medicineName().trim(); dosage = request.dosage().trim(); route = request.route().trim();
        frequency = request.frequency().trim(); duration = request.duration().trim(); quantity = request.quantity();
        instructions = request.instructions() == null || request.instructions().isBlank() ? null : request.instructions().trim(); startDate = request.startDate();
    }
    void setPrescription(Prescription value) { prescription = value; }
    void setItemOrder(int value) { itemOrder = value; }
    public Long getId() { return id; } public String getMedicineName() { return medicineName; } public String getDosage() { return dosage; }
    public int getItemOrder() { return itemOrder; }
    public String getRoute() { return route; } public String getFrequency() { return frequency; } public String getDuration() { return duration; }
    public BigDecimal getQuantity() { return quantity; } public String getInstructions() { return instructions; } public LocalDate getStartDate() { return startDate; }
}
