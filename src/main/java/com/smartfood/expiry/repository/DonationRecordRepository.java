package com.smartfood.expiry.repository;

import com.smartfood.expiry.entity.DonationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DonationRecordRepository extends JpaRepository<DonationRecord, Long> {

    List<DonationRecord> findAllByOrderByDonationDateDesc();

    List<DonationRecord> findByDonationDateBetweenOrderByDonationDateDesc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT COALESCE(SUM(d.quantity), 0) FROM DonationRecord d")
    Long sumTotalDonatedQuantity();
}
