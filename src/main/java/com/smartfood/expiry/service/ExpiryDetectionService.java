package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ExpiryDetectionService {

    private final ProductBatchRepository batchRepository;
    private final CategoryRepository categoryRepository;
    private final int defaultWarningDays;

    public ExpiryDetectionService(ProductBatchRepository batchRepository,
                                  CategoryRepository categoryRepository,
                                  @Value("${app.expiry.default-warning-days:7}") int defaultWarningDays) {
        this.batchRepository = batchRepository;
        this.categoryRepository = categoryRepository;
        this.defaultWarningDays = defaultWarningDays;
    }

    public int getEffectiveWarningDays(Category category) {
        if (category != null && category.getDefaultWarningDays() != null && category.getDefaultWarningDays() > 0) {
            return category.getDefaultWarningDays();
        }
        return defaultWarningDays;
    }

    public ExpiryStatus detectStatus(ProductBatch batch, LocalDate referenceDate) {
        if (batch == null || batch.getExpiryDate() == null) {
            return ExpiryStatus.SAFE;
        }
        Category cat = (batch.getProduct() != null) ? batch.getProduct().getCategory() : null;
        int warningDays = getEffectiveWarningDays(cat);
        return batch.calculateStatus(referenceDate, warningDays);
    }

    public BatchItemViewDto wrapBatch(ProductBatch batch, LocalDate referenceDate) {
        Category cat = (batch.getProduct() != null) ? batch.getProduct().getCategory() : null;
        int warningDays = getEffectiveWarningDays(cat);
        return new BatchItemViewDto(batch, referenceDate, warningDays);
    }

    public List<BatchItemViewDto> getAllActiveBatchViews(LocalDate referenceDate) {
        List<ProductBatch> batches = batchRepository.findAllActiveBatchesWithStock();
        return batches.stream()
                .map(b -> wrapBatch(b, referenceDate))
                .collect(Collectors.toList());
    }

    public List<BatchItemViewDto> getExpiredBatches(LocalDate referenceDate) {
        return getAllActiveBatchViews(referenceDate).stream()
                .filter(b -> b.getStatus() == ExpiryStatus.EXPIRED)
                .collect(Collectors.toList());
    }

    public List<BatchItemViewDto> getExpiringTodayBatches(LocalDate referenceDate) {
        return getAllActiveBatchViews(referenceDate).stream()
                .filter(b -> b.getDaysRemaining() == 0)
                .collect(Collectors.toList());
    }

    public List<BatchItemViewDto> getNearExpiryBatches(LocalDate referenceDate) {
        return getAllActiveBatchViews(referenceDate).stream()
                .filter(b -> b.getStatus() == ExpiryStatus.NEAR_EXPIRY)
                .collect(Collectors.toList());
    }

    public List<BatchItemViewDto> getSafeBatches(LocalDate referenceDate) {
        return getAllActiveBatchViews(referenceDate).stream()
                .filter(b -> b.getStatus() == ExpiryStatus.SAFE)
                .collect(Collectors.toList());
    }

    public BigDecimal calculateStockValueAtRisk(LocalDate referenceDate) {
        return getAllActiveBatchViews(referenceDate).stream()
                .filter(b -> b.getStatus() == ExpiryStatus.EXPIRED || b.getStatus() == ExpiryStatus.NEAR_EXPIRY)
                .map(BatchItemViewDto::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
