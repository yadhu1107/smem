package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BarcodeLookupDto;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BarcodeService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final ExpiryDetectionService expiryDetectionService;

    public BarcodeService(ProductRepository productRepository,
                          ProductBatchRepository batchRepository,
                          ExpiryDetectionService expiryDetectionService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.expiryDetectionService = expiryDetectionService;
    }

    public BarcodeLookupDto lookupBarcode(String barcode) {
        BarcodeLookupDto dto = new BarcodeLookupDto();
        dto.setBarcode(barcode != null ? barcode.trim() : "");

        if (barcode == null || barcode.trim().isEmpty()) {
            dto.setFound(false);
            return dto;
        }

        Optional<Product> productOpt = productRepository.findByBarcode(barcode.trim());
        if (productOpt.isEmpty()) {
            dto.setFound(false);
            return dto;
        }

        Product product = productOpt.get();
        dto.setFound(true);
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setCategoryName(product.getCategory() != null ? product.getCategory().getName() : "");
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);
        dto.setBrand(product.getBrand());
        dto.setUnit(product.getUnit());
        dto.setPurchasePrice(product.getPurchasePrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setMinStockQuantity(product.getMinStockQuantity());

        List<ProductBatch> batches = batchRepository.findByProductId(product.getId());
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (ProductBatch b : batches) {
            ExpiryStatus status = expiryDetectionService.detectStatus(b, LocalDate.now());
            dto.getExistingBatches().add(new BarcodeLookupDto.BatchSummaryDto(
                    b.getId(),
                    b.getBatchNumber(),
                    b.getQuantity(),
                    b.getExpiryDate() != null ? b.getExpiryDate().format(dtf) : "N/A",
                    status.getDisplayName()
            ));
        }

        return dto;
    }
}
