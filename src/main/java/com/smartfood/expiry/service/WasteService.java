package com.smartfood.expiry.service;

import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.entity.WasteRecord;
import com.smartfood.expiry.enums.MovementType;
import com.smartfood.expiry.enums.WasteReason;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.StockMovementRepository;
import com.smartfood.expiry.repository.WasteRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class WasteService {

    private final WasteRecordRepository wasteRecordRepository;
    private final ProductBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;

    public WasteService(WasteRecordRepository wasteRecordRepository,
                        ProductBatchRepository batchRepository,
                        StockMovementRepository movementRepository) {
        this.wasteRecordRepository = wasteRecordRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public List<WasteRecord> getAllWasteRecords() {
        return wasteRecordRepository.findAllByOrderByWasteDateDesc();
    }

    @Transactional(readOnly = true)
    public List<WasteRecord> getWasteRecordsBetween(LocalDate start, LocalDate end) {
        return wasteRecordRepository.findByWasteDateBetweenOrderByWasteDateDesc(start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalEstimatedLoss() {
        return wasteRecordRepository.sumTotalEstimatedLoss();
    }

    @Transactional(readOnly = true)
    public Long getTotalWastedQuantity() {
        return wasteRecordRepository.sumTotalWastedQuantity();
    }

    public WasteRecord recordWaste(Long batchId, int quantity, WasteReason reason,
                                  LocalDate wasteDate, String notes, User user) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Wasted quantity must be greater than zero");
        }
        if (reason == null) {
            throw new IllegalArgumentException("Waste reason is required");
        }

        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

        if (batch.getQuantity() < quantity) {
            throw new IllegalArgumentException("Cannot dispose " + quantity + " units. Only " +
                    batch.getQuantity() + " units available in batch " + batch.getBatchNumber());
        }

        if (wasteDate == null) {
            wasteDate = LocalDate.now();
        }

        int prevQty = batch.getQuantity();
        int newQty = prevQty - quantity;
        batch.setQuantity(newQty);
        batchRepository.save(batch);

        BigDecimal costPerUnit = (batch.getCostPerUnit() != null) ? batch.getCostPerUnit() : BigDecimal.ZERO;
        BigDecimal estimatedLoss = costPerUnit.multiply(BigDecimal.valueOf(quantity));

        WasteRecord wasteRecord = new WasteRecord(batch, quantity, reason, wasteDate, user, estimatedLoss, notes);
        WasteRecord savedWaste = wasteRecordRepository.save(wasteRecord);

        // Record Stock Movement
        StockMovement movement = new StockMovement(
                batch,
                MovementType.WASTE_OUT,
                quantity,
                prevQty,
                newQty,
                "Food waste disposal: " + reason.getLabel(),
                user,
                notes
        );
        movementRepository.save(movement);

        return savedWaste;
    }
}
