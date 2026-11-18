package com.gbkhealthcare.billing;
import org.springframework.data.repository.Repository;import java.math.BigDecimal;import java.util.List;
public interface BillingRefundRepository extends Repository<BillingRefund,Long>{BillingRefund save(BillingRefund refund);List<BillingRefund> findAllByPayment_IdOrderByRefundedAtAsc(Long paymentId);}
