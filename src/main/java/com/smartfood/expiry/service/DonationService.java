package com.smartfood.expiry.service;

import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.entity.DonationRecord;
import com.smartfood.expiry.enums.DonationStatus;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.enums.MovementType;
import com.smartfood.expiry.repository.DonationRecordRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class DonationService {

    private final DonationRecordRepository donationRecordRepository;
    private final ProductBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final ExpiryDetectionService expiryDetectionService;

    public DonationService(DonationRecordRepository donationRecordRepository,
                           ProductBatchRepository batchRepository,
                           StockMovementRepository movementRepository,
                           ExpiryDetectionService expiryDetectionService) {
        this.donationRecordRepository = donationRecordRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.expiryDetectionService = expiryDetectionService;
    }

    @Transactional(readOnly = true)
    public List<DonationRecord> getAllDonations() {
        return donationRecordRepository.findAllByOrderByDonationDateDesc();
    }

    @Transactional(readOnly = true)
    public Long getTotalDonatedQuantity() {
        return donationRecordRepository.sumTotalDonatedQuantity();
    }

    public DonationRecord recordDonation(Long batchId, String recipientOrg, int quantity,
                                         LocalDate donationDate, String notes, User user) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Donation quantity must be greater than zero");
        }
        if (recipientOrg == null || recipientOrg.isBlank()) {
            throw new IllegalArgumentException("Recipient organization is required");
        }

        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

        // Strict Safety Validation: Never donate expired food!
        ExpiryStatus status = expiryDetectionService.detectStatus(batch, LocalDate.now());
        if (status == ExpiryStatus.EXPIRED) {
            throw new IllegalStateException("Food safety violation: Expired products cannot be marked for donation. Please route to Food Waste disposal instead.");
        }

        if (batch.getQuantity() < quantity) {
            throw new IllegalArgumentException("Cannot donate " + quantity + " units. Only " +
                    batch.getQuantity() + " units available in batch " + batch.getBatchNumber());
        }

        if (donationDate == null) {
            donationDate = LocalDate.now();
        }

        int prevQty = batch.getQuantity();
        int newQty = prevQty - quantity;
        batch.setQuantity(newQty);
        batchRepository.save(batch);

        DonationRecord donationRecord = new DonationRecord(
                batch,
                recipientOrg,
                quantity,
                donationDate,
                DonationStatus.COMPLETED,
                user,
                notes
        );
        DonationRecord savedDonation = donationRecordRepository.save(donationRecord);

        // Record Stock Movement
        StockMovement movement = new StockMovement(
                batch,
                MovementType.DONATION_OUT,
                quantity,
                prevQty,
                newQty,
                "Surplus food donation to " + recipientOrg,
                user,
                notes
        );
        movementRepository.save(movement);

        return savedDonation;
    }
}
