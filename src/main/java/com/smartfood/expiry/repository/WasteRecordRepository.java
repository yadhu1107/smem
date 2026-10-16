package com.smartfood.expiry.repository;

import com.smartfood.expiry.entity.WasteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WasteRecordRepository extends JpaRepository<WasteRecord, Long> {

    List<WasteRecord> findAllByOrderByWasteDateDesc();

    List<WasteRecord> findByWasteDateBetweenOrderByWasteDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(w.estimatedCostLoss), 0) FROM WasteRecord w")
    BigDecimal sumTotalEstimatedLoss();

    @Query("SELECT COALESCE(SUM(w.quantity), 0) FROM WasteRecord w")
    Long sumTotalWastedQuantity();
}
