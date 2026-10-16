package com.smartfood.expiry.service;

import com.smartfood.expiry.entity.*;
import com.smartfood.expiry.enums.MovementType;
import com.smartfood.expiry.enums.RoleType;
import com.smartfood.expiry.enums.WasteReason;
import com.smartfood.expiry.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockValidationAndMovementTest {

    @Mock
    private ProductBatchRepository batchRepository;

    @Mock
    private StockMovementRepository movementRepository;

    @Mock
    private WasteRecordRepository wasteRecordRepository;

    @Mock
    private DonationRecordRepository donationRecordRepository;

    @Mock
    private ExpiryDetectionService expiryDetectionService;

    private BatchService batchService;
    private WasteService wasteService;
    private DonationService donationService;

    private ProductBatch sampleBatch;
    private User testUser;
    private final LocalDate today = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        batchService = new BatchService(batchRepository, movementRepository);
        wasteService = new WasteService(wasteRecordRepository, batchRepository, movementRepository);
        donationService = new DonationService(donationRecordRepository, batchRepository, movementRepository, expiryDetectionService);

        Category dairy = new Category("Dairy", "DAIRY", 3, "Dairy");
        Product milk = new Product("Milk", dairy, "Amul", "12345678", "liters", 10, new BigDecimal("40.00"), new BigDecimal("50.00"));

        sampleBatch = new ProductBatch(milk, "BAT-001", 10, 10, today.minusDays(5), today.plusDays(3),
                new BigDecimal("40.00"), new BigDecimal("50.00"), "Supplier");
        sampleBatch.setId(1L);

        testUser = new User("staff1", "staff@test.com", "pass", "Staff Member", RoleType.ROLE_STAFF);
    }

    @Test
    @DisplayName("Prevent adjustment that would lead to negative stock")
    void testNegativeStockPrevention() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));

        // Attempting to deduct 15 units when only 10 are in stock
        assertThrows(IllegalArgumentException.class, () -> {
            batchService.adjustStock(1L, -15, MovementType.ADJUSTMENT_OUT, "Inventory loss", "Audit test", testUser);
        });

        // Ensure batch quantity remains unchanged
        assertEquals(10, sampleBatch.getQuantity());
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    @DisplayName("Successful stock adjustment logs StockMovement audit trail")
    void testStockAdjustmentAuditTrail() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));
        when(batchRepository.save(any(ProductBatch.class))).thenAnswer(i -> i.getArgument(0));

        // Deduct 4 units
        ProductBatch updated = batchService.adjustStock(1L, -4, MovementType.ADJUSTMENT_OUT, "Damaged in aisle", "Test", testUser);

        assertEquals(6, updated.getQuantity());

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(movementRepository).save(movementCaptor.capture());

        StockMovement savedMovement = movementCaptor.getValue();
        assertEquals(4, savedMovement.getQuantity());
        assertEquals(10, savedMovement.getPreviousQuantity());
        assertEquals(6, savedMovement.getNewQuantity());
        assertEquals(MovementType.ADJUSTMENT_OUT, savedMovement.getMovementType());
    }

    @Test
    @DisplayName("Waste disposal reduces batch stock, calculates cost loss, and creates movement")
    void testFoodWasteRecording() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));
        when(wasteRecordRepository.save(any(WasteRecord.class))).thenAnswer(i -> i.getArgument(0));

        // Discard 3 units due to packaging defect
        WasteRecord waste = wasteService.recordWaste(1L, 3, WasteReason.PACKAGING_DEFECT, today, "Carton crushed", testUser);

        assertEquals(7, sampleBatch.getQuantity()); // 10 - 3
        assertEquals(new BigDecimal("120.00"), waste.getEstimatedCostLoss()); // 3 * 40.00

        verify(movementRepository).save(argThat(m ->
                m.getMovementType() == MovementType.WASTE_OUT &&
                m.getQuantity() == 3 &&
                m.getPreviousQuantity() == 10 &&
                m.getNewQuantity() == 7
        ));
    }

    @Test
    @DisplayName("Food safety rule: Reject donation if batch is expired")
    void testRejectExpiredDonation() {
        when(batchRepository.findById(1L)).thenReturn(Optional.of(sampleBatch));
        when(expiryDetectionService.detectStatus(sampleBatch, LocalDate.now())).thenReturn(com.smartfood.expiry.enums.ExpiryStatus.EXPIRED);

        assertThrows(IllegalStateException.class, () -> {
            donationService.recordDonation(1L, "Local Shelter", 5, today, "Surplus food", testUser);
        });

        assertEquals(10, sampleBatch.getQuantity());
        verify(donationRecordRepository, never()).save(any(DonationRecord.class));
    }
}
