package com.smartfood.expiry.repository;

import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    List<ProductBatch> findByProductId(Long productId);

    List<ProductBatch> findByProductOrderByExpiryDateAsc(Product product);

    Optional<ProductBatch> findByProductAndBatchNumber(Product product, String batchNumber);

    // FEFO: strictly ordered by earliest expiry date where quantity > 0
    List<ProductBatch> findByQuantityGreaterThanOrderByExpiryDateAsc(Integer minQty);

    List<ProductBatch> findByProductAndQuantityGreaterThanOrderByExpiryDateAsc(Product product, Integer minQty);

    // Batches expiring before a date (e.g. expired batches before today)
    List<ProductBatch> findByExpiryDateBeforeAndQuantityGreaterThanOrderByExpiryDateAsc(LocalDate date, Integer minQty);

    // Batches expiring on a specific date (e.g. today)
    List<ProductBatch> findByExpiryDateAndQuantityGreaterThanOrderByExpiryDateAsc(LocalDate date, Integer minQty);

    // Batches expiring between two dates (e.g. near expiry)
    List<ProductBatch> findByExpiryDateBetweenAndQuantityGreaterThanOrderByExpiryDateAsc(LocalDate startDate, LocalDate endDate, Integer minQty);

    // Filter by product active status and quantity > 0
    @Query("SELECT b FROM ProductBatch b WHERE b.product.active = true AND b.quantity > 0 ORDER BY b.expiryDate ASC")
    List<ProductBatch> findAllActiveBatchesWithStock();

    @Query("SELECT SUM(b.quantity) FROM ProductBatch b WHERE b.product.id = :productId AND b.quantity > 0 AND b.expiryDate >= :today")
    Integer sumValidStockByProductId(@Param("productId") Long productId, @Param("today") LocalDate today);

    @Query("SELECT SUM(b.quantity) FROM ProductBatch b WHERE b.quantity > 0 AND b.expiryDate >= :today")
    Long sumTotalActiveStock(@Param("today") LocalDate today);

    @Query("SELECT b FROM ProductBatch b WHERE b.product.category.id = :categoryId AND b.quantity > 0 ORDER BY b.expiryDate ASC")
    List<ProductBatch> findByCategoryIdWithStock(@Param("categoryId") Long categoryId);
}
