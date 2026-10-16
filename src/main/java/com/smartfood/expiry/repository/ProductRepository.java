package com.smartfood.expiry.repository;

import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    boolean existsByBarcode(String barcode);

    List<Product> findByActiveTrueOrderByNameAsc();

    List<Product> findByCategoryAndActiveTrueOrderByNameAsc(Category category);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProducts(@Param("keyword") String keyword);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(p.barcode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchProductsWithCategory(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);

    long countByActiveTrue();
}
