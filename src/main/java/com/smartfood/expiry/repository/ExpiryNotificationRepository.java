package com.smartfood.expiry.repository;

import com.smartfood.expiry.entity.ExpiryNotification;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpiryNotificationRepository extends JpaRepository<ExpiryNotification, Long> {

    List<ExpiryNotification> findByReviewedFalseOrderByAlertDateDesc();

    List<ExpiryNotification> findAllByOrderByAlertDateDesc();

    boolean existsByBatchAndAlertDateAndAlertType(ProductBatch batch, LocalDate alertDate, ExpiryStatus alertType);

    List<ExpiryNotification> findByBatchOrderByAlertDateDesc(ProductBatch batch);

    long countByReviewedFalse();
}
