package com.gbkhealthcare.billing;
import org.springframework.data.repository.Repository;import java.util.List;
public interface BillingPaymentRepository extends Repository<BillingPayment,Long>{BillingPayment save(BillingPayment payment);java.util.Optional<BillingPayment> findById(Long id);List<BillingPayment> findAllByInvoice_IdOrderByPaidAtAsc(Long invoiceId);}
