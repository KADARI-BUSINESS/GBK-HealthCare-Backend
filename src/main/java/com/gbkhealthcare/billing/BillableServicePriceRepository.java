package com.gbkhealthcare.billing;
import org.springframework.data.repository.Repository;import java.util.*;
public interface BillableServicePriceRepository extends Repository<BillableServicePrice,Long>{BillableServicePrice save(BillableServicePrice price);Optional<BillableServicePrice> findTopByService_IdOrderByVersionDesc(Long serviceId);int countByService_Id(Long serviceId);List<BillableServicePrice> findAllByService_IdOrderByVersionDesc(Long serviceId);}
