package com.smartfood.expiry.repository;

import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    List<StockMovement> findByBatchOrderByMovementDateDesc(ProductBatch batch);

    List<StockMovement> findAllByOrderByMovementDateDesc();

    List<StockMovement> findByMovementDateBetweenOrderByMovementDateDesc(LocalDateTime start, LocalDateTime end);
}
