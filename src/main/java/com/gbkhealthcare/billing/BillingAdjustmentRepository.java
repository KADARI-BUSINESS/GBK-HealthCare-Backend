package com.gbkhealthcare.billing;
import org.springframework.data.repository.Repository;import java.math.BigDecimal;import java.util.List;
public interface BillingAdjustmentRepository extends Repository<BillingAdjustment,Long>{BillingAdjustment save(BillingAdjustment adjustment);List<BillingAdjustment> findAllByInvoice_IdOrderByCreatedAtAsc(Long invoiceId);}
