package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.ProductViewDto;
import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final CategoryRepository categoryRepository;
    private final ExpiryDetectionService expiryDetectionService;

    public ProductService(ProductRepository productRepository,
                          ProductBatchRepository batchRepository,
                          CategoryRepository categoryRepository,
                          ExpiryDetectionService expiryDetectionService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.categoryRepository = categoryRepository;
        this.expiryDetectionService = expiryDetectionService;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return productRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return Optional.empty();
        }
        return productRepository.findByBarcode(barcode.trim());
    }

    @Transactional(readOnly = true)
    public boolean existsByBarcode(String barcode) {
        return productRepository.existsByBarcode(barcode);
    }

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

        // Check if there are active batches with stock
        List<ProductBatch> batches = batchRepository.findByProductId(id);
        int totalRemainingStock = batches.stream().mapToInt(ProductBatch::getQuantity).sum();
        if (totalRemainingStock > 0) {
            throw new IllegalStateException("Cannot delete product with active inventory (" + totalRemainingStock + " units remaining). Adjust stock or dispose batches first.");
        }

        // Soft delete / deactivate to maintain referential integrity with movements
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductViewDto getProductViewDto(Product product, LocalDate referenceDate) {
        List<ProductBatch> batches = batchRepository.findByProductId(product.getId());

        int totalValidStock = 0;
        int totalExpiredStock = 0;
        LocalDate earliest = null;

        for (ProductBatch b : batches) {
            if (b.getQuantity() > 0) {
                if (b.getExpiryDate().isBefore(referenceDate)) {
                    totalExpiredStock += b.getQuantity();
                } else {
                    totalValidStock += b.getQuantity();
                    if (earliest == null || b.getExpiryDate().isBefore(earliest)) {
                        earliest = b.getExpiryDate();
                    }
                }
            }
        }

        return new ProductViewDto(product, totalValidStock, totalExpiredStock, batches.size(), earliest);
    }

    @Transactional(readOnly = true)
    public List<ProductViewDto> searchAndFilterProducts(String keyword, Long categoryId, String expiryFilter, LocalDate referenceDate) {
        List<Product> products;

        if (keyword != null && !keyword.trim().isEmpty() && categoryId != null) {
            products = productRepository.searchProductsWithCategory(keyword.trim(), categoryId);
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.searchProducts(keyword.trim());
        } else if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId).orElse(null);
            products = (cat != null) ? productRepository.findByCategoryAndActiveTrueOrderByNameAsc(cat) : productRepository.findByActiveTrueOrderByNameAsc();
        } else {
            products = productRepository.findByActiveTrueOrderByNameAsc();
        }

        List<ProductViewDto> views = products.stream()
                .map(p -> getProductViewDto(p, referenceDate))
                .collect(Collectors.toList());

        // Filter by Expiry Status if requested
        if (expiryFilter != null && !expiryFilter.trim().isEmpty()) {
            switch (expiryFilter.toUpperCase()) {
                case "LOW_STOCK":
                    views = views.stream().filter(ProductViewDto::isLowStock).collect(Collectors.toList());
                    break;
                case "OUT_OF_STOCK":
                    views = views.stream().filter(ProductViewDto::isOutOfStock).collect(Collectors.toList());
                    break;
                case "EXPIRED":
                    views = views.stream().filter(v -> v.getTotalExpiredStock() > 0).collect(Collectors.toList());
                    break;
                case "HAS_STOCK":
                    views = views.stream().filter(v -> v.getTotalValidStock() > 0).collect(Collectors.toList());
                    break;
                default:
                    break;
            }
        }

        return views;
    }

    @Transactional(readOnly = true)
    public List<ProductViewDto> getLowStockProducts(LocalDate referenceDate) {
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(p -> getProductViewDto(p, referenceDate))
                .filter(ProductViewDto::isLowStock)
                .collect(Collectors.toList());
    }
}
