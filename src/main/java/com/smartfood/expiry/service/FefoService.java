package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FefoService {

    private final ProductBatchRepository batchRepository;
    private final ExpiryDetectionService expiryDetectionService;

    public FefoService(ProductBatchRepository batchRepository, ExpiryDetectionService expiryDetectionService) {
        this.batchRepository = batchRepository;
        this.expiryDetectionService = expiryDetectionService;
    }

    /**
     * Returns active batches ordered by earliest expiry (FEFO).
     * Strictly filters out expired products (cannot recommend expired items for rotation or sale).
     */
    public List<BatchItemViewDto> getFefoRecommendations(LocalDate referenceDate) {
        List<ProductBatch> eligibleBatches = batchRepository.findByQuantityGreaterThanOrderByExpiryDateAsc(0);
        return eligibleBatches.stream()
                .filter(b -> b.getProduct() != null && b.getProduct().isActive())
                .map(b -> expiryDetectionService.wrapBatch(b, referenceDate))
                .filter(view -> view.getStatus() != ExpiryStatus.EXPIRED) // Strictly exclude expired
                .collect(Collectors.toList());
    }

    /**
     * Returns FEFO recommendations for a specific product.
     */
    public List<BatchItemViewDto> getFefoRecommendationsForProduct(Product product, LocalDate referenceDate) {
        List<ProductBatch> eligibleBatches = batchRepository.findByProductAndQuantityGreaterThanOrderByExpiryDateAsc(product, 0);
        return eligibleBatches.stream()
                .map(b -> expiryDetectionService.wrapBatch(b, referenceDate))
                .filter(view -> view.getStatus() != ExpiryStatus.EXPIRED)
                .collect(Collectors.toList());
    }
}
