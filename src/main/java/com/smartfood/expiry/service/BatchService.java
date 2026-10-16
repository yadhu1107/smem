package com.smartfood.expiry.service;

import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.MovementType;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.StockMovementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BatchService {

    private final ProductBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;

    public BatchService(ProductBatchRepository batchRepository, StockMovementRepository movementRepository) {
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
    }

    public List<ProductBatch> getBatchesByProduct(Long productId) {
        return batchRepository.findByProductId(productId);
    }

    public Optional<ProductBatch> getBatchById(Long id) {
        return batchRepository.findById(id);
    }

    public ProductBatch createBatch(Product product, String batchNumber, int quantity,
                                    LocalDate purchaseDate, LocalDate expiryDate,
                                    BigDecimal costPerUnit, BigDecimal sellingPricePerUnit,
                                    String supplier, String notes, User user) {
        if (product == null) {
            throw new IllegalArgumentException("Product is required");
        }
        if (batchNumber == null || batchNumber.isBlank()) {
            throw new IllegalArgumentException("Batch number is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (purchaseDate == null) {
            purchaseDate = LocalDate.now();
        }
        if (expiryDate == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        if (expiryDate.isBefore(purchaseDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before the purchase date");
        }
        if (costPerUnit != null && costPerUnit.signum() < 0) {
            throw new IllegalArgumentException("Unit purchase cost cannot be negative");
        }
        if (sellingPricePerUnit != null && sellingPricePerUnit.signum() < 0) {
            throw new IllegalArgumentException("Unit selling price cannot be negative");
        }

        ProductBatch batch = new ProductBatch();
        batch.setProduct(product);
        batch.setBatchNumber(batchNumber);
        batch.setQuantity(quantity);
        batch.setInitialQuantity(quantity);
        batch.setPurchaseDate(purchaseDate);
        batch.setExpiryDate(expiryDate);
        batch.setCostPerUnit(costPerUnit != null ? costPerUnit : (product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO));
        batch.setSellingPricePerUnit(sellingPricePerUnit != null ? sellingPricePerUnit : (product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO));
        batch.setSupplier(supplier);
        batch.setNotes(notes);

        ProductBatch savedBatch = batchRepository.save(batch);

        // Record Stock Movement
        StockMovement movement = new StockMovement(
                savedBatch,
                MovementType.PURCHASE_IN,
                quantity,
                0,
                quantity,
                "New batch stock inward",
                user,
                notes
        );
        movementRepository.save(movement);

        return savedBatch;
    }

    public ProductBatch adjustStock(Long batchId, int quantityChange, MovementType movementType,
                                    String reason, String notes, User user) {
        if (quantityChange == 0) {
            throw new IllegalArgumentException("Adjustment quantity must be greater than zero");
        }
        if (movementType == null) {
            throw new IllegalArgumentException("Stock movement type is required");
        }
        ProductBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

        int previousQuantity = batch.getQuantity();
        int newQuantity = previousQuantity + quantityChange;

        if (newQuantity < 0) {
            throw new IllegalArgumentException("Adjustment would cause negative stock! Current stock: " +
                    previousQuantity + ", requested change: " + quantityChange);
        }

        batch.setQuantity(newQuantity);
        ProductBatch updatedBatch = batchRepository.save(batch);

        StockMovement movement = new StockMovement(
                updatedBatch,
                movementType,
                Math.abs(quantityChange),
                previousQuantity,
                newQuantity,
                reason,
                user,
                notes
        );
        movementRepository.save(movement);

        return updatedBatch;
    }

    public List<StockMovement> getMovementsForBatch(ProductBatch batch) {
        return movementRepository.findByBatchOrderByMovementDateDesc(batch);
    }
}
