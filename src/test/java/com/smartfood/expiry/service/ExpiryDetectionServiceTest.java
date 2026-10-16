package com.smartfood.expiry.service;

import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ExpiryDetectionServiceTest {

    @Mock
    private ProductBatchRepository batchRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ExpiryDetectionService expiryDetectionService;

    private final LocalDate today = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        expiryDetectionService = new ExpiryDetectionService(batchRepository, categoryRepository, 7);
    }

    @Test
    @DisplayName("Batch expiring 2 days ago should be marked as EXPIRED")
    void testExpiredBatch() {
        ProductBatch batch = new ProductBatch();
        batch.setExpiryDate(today.minusDays(2));
        batch.setQuantity(10);

        ExpiryStatus status = expiryDetectionService.detectStatus(batch, today);
        assertEquals(ExpiryStatus.EXPIRED, status);
    }

    @Test
    @DisplayName("Batch expiring today should be marked as NEAR_EXPIRY")
    void testExpiringTodayBatch() {
        ProductBatch batch = new ProductBatch();
        batch.setExpiryDate(today);
        batch.setQuantity(10);

        ExpiryStatus status = expiryDetectionService.detectStatus(batch, today);
        assertEquals(ExpiryStatus.NEAR_EXPIRY, status);
    }

    @Test
    @DisplayName("Batch within default warning period (3 days left <= 7) should be NEAR_EXPIRY")
    void testNearExpiryBatchWithinDefaultWarning() {
        ProductBatch batch = new ProductBatch();
        batch.setExpiryDate(today.plusDays(3));
        batch.setQuantity(10);

        ExpiryStatus status = expiryDetectionService.detectStatus(batch, today);
        assertEquals(ExpiryStatus.NEAR_EXPIRY, status);
    }

    @Test
    @DisplayName("Batch beyond warning period (10 days left > 7) should be SAFE")
    void testSafeBatchBeyondDefaultWarning() {
        ProductBatch batch = new ProductBatch();
        batch.setExpiryDate(today.plusDays(10));
        batch.setQuantity(10);

        ExpiryStatus status = expiryDetectionService.detectStatus(batch, today);
        assertEquals(ExpiryStatus.SAFE, status);
    }

    @Test
    @DisplayName("Category-specific warning period: Bakery with 2 days warning makes 4 days left SAFE")
    void testCategorySpecificWarningPeriod() {
        Category bakery = new Category("Bakery", "BAKERY", 2, "Bakery products");
        Product bread = new Product("Bread", bakery, "Brand", "12345678", "pcs", 10, BigDecimal.TEN, BigDecimal.TEN);

        ProductBatch batch = new ProductBatch();
        batch.setProduct(bread);
        batch.setExpiryDate(today.plusDays(4)); // 4 days remaining > 2 days bakery warning
        batch.setQuantity(5);

        ExpiryStatus status = expiryDetectionService.detectStatus(batch, today);
        assertEquals(ExpiryStatus.SAFE, status);

        // If expiry is within 2 days (e.g. 1 day left), it becomes NEAR_EXPIRY
        batch.setExpiryDate(today.plusDays(1));
        ExpiryStatus nearStatus = expiryDetectionService.detectStatus(batch, today);
        assertEquals(ExpiryStatus.NEAR_EXPIRY, nearStatus);
    }
}
