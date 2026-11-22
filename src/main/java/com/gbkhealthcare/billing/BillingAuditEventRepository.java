package com.gbkhealthcare.billing;
import org.springframework.data.repository.Repository;import java.util.List;
public interface BillingAuditEventRepository extends Repository<BillingAuditEvent,Long>{BillingAuditEvent save(BillingAuditEvent event);List<BillingAuditEvent> findAllByInvoiceIdOrderByOccurredAtAscIdAsc(Long invoiceId);}
