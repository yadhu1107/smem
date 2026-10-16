package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.ProductBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FefoServiceTest {

    @Mock
    private ProductBatchRepository batchRepository;

    @Mock
    private ExpiryDetectionService expiryDetectionService;

    private FefoService fefoService;

    private final LocalDate today = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void setUp() {
        fefoService = new FefoService(batchRepository, expiryDetectionService);
    }

    @Test
    @DisplayName("FEFO recommendation strictly sorts by earliest expiry and excludes expired batches")
    void testFefoOrderingAndExclusion() {
        Category category = new Category("Dairy", "DAIRY", 3, "Dairy products");
        Product milk = new Product("Milk 1L", category, "Amul", "89012345", "liters", 10, BigDecimal.TEN, BigDecimal.TEN);

        ProductBatch expiredBatch = new ProductBatch(milk, "BAT-EXPIRED", 5, 5, today.minusDays(10), today.minusDays(1), BigDecimal.TEN, BigDecimal.TEN, "Supplier");
        ProductBatch nearBatch = new ProductBatch(milk, "BAT-NEAR", 10, 10, today.minusDays(2), today.plusDays(2), BigDecimal.TEN, BigDecimal.TEN, "Supplier");
        ProductBatch safeBatch = new ProductBatch(milk, "BAT-SAFE", 20, 20, today.minusDays(1), today.plusDays(10), BigDecimal.TEN, BigDecimal.TEN, "Supplier");

        // Mock DB returning batches ordered by expiry
        when(batchRepository.findByQuantityGreaterThanOrderByExpiryDateAsc(0))
                .thenReturn(Arrays.asList(expiredBatch, nearBatch, safeBatch));

        when(expiryDetectionService.wrapBatch(expiredBatch, today))
                .thenReturn(new BatchItemViewDto(expiredBatch, today, 3));
        when(expiryDetectionService.wrapBatch(nearBatch, today))
                .thenReturn(new BatchItemViewDto(nearBatch, today, 3));
        when(expiryDetectionService.wrapBatch(safeBatch, today))
                .thenReturn(new BatchItemViewDto(safeBatch, today, 3));

        List<BatchItemViewDto> fefoList = fefoService.getFefoRecommendations(today);

        // Expired batch MUST be excluded from FEFO recommendations!
        assertEquals(2, fefoList.size());
        assertEquals("BAT-NEAR", fefoList.get(0).getBatch().getBatchNumber());
        assertEquals(ExpiryStatus.NEAR_EXPIRY, fefoList.get(0).getStatus());

        assertEquals("BAT-SAFE", fefoList.get(1).getBatch().getBatchNumber());
        assertEquals(ExpiryStatus.SAFE, fefoList.get(1).getStatus());
    }
}
