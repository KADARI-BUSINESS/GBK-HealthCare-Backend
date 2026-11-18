package com.gbkhealthcare.billing;
import org.springframework.data.repository.Repository;import java.util.List;
public interface BillingInvoiceLineRepository extends Repository<BillingInvoiceLine,Long>{List<BillingInvoiceLine> findAllByInvoice_IdOrderByIdAsc(Long invoiceId);}
