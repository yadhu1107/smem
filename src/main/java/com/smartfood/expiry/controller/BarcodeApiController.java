package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BarcodeLookupDto;
import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.service.BarcodeService;
import com.smartfood.expiry.service.BatchService;
import com.smartfood.expiry.service.ProductService;
import com.smartfood.expiry.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/barcode")
public class BarcodeApiController {

    private final BarcodeService barcodeService;
    private final ProductService productService;
    private final BatchService batchService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;

    public BarcodeApiController(BarcodeService barcodeService,
                                ProductService productService,
                                BatchService batchService,
                                UserService userService,
                                CategoryRepository categoryRepository) {
        this.barcodeService = barcodeService;
        this.productService = productService;
        this.batchService = batchService;
        this.userService = userService;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/lookup")
    public ResponseEntity<BarcodeLookupDto> lookup(@RequestParam("barcode") String barcode) {
        BarcodeLookupDto result = barcodeService.lookupBarcode(barcode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/demo-list")
    public ResponseEntity<List<Map<String, Object>>> getDemoBarcodes() {
        List<Product> products = productService.getAllActiveProducts().stream()
                .filter(p -> p.getBarcode() != null && p.getBarcode().matches("8901262(0[1-9]|1[0-9]|20)[0-9]{3}"))
                .sorted(Comparator.comparing(Product::getBarcode))
                .limit(20)
                .toList();
        List<Map<String, Object>> demoList = new ArrayList<>();

        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("barcode", p.getBarcode());
            item.put("name", p.getName());
            item.put("brand", p.getBrand());
            item.put("category", p.getCategory() != null ? p.getCategory().getName() : "General");
            item.put("unit", p.getUnit());
            item.put("sellingPrice", p.getSellingPrice());
            item.put("batchesCount", batchService.getBatchesByProduct(p.getId()).size());
            demoList.add(item);
        }

        return ResponseEntity.ok(demoList);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categories = categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", category.getId());
                    item.put("name", category.getName());
                    item.put("warningDays", category.getDefaultWarningDays());
                    return item;
                })
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/quick-batch")
    public ResponseEntity<Map<String, Object>> quickBatch(@RequestParam("productId") Long productId,
                                                          @RequestParam("batchNumber") String batchNumber,
                                                          @RequestParam("quantity") Integer quantity,
                                                          @RequestParam("expiryDate") String expiryDateStr,
                                                          @RequestParam(value = "costPerUnit", required = false) BigDecimal costPerUnit,
                                                          @RequestParam(value = "supplier", required = false) String supplier,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        Map<String, Object> response = new HashMap<>();
        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            LocalDate expiryDate = LocalDate.parse(expiryDateStr);
            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;

            ProductBatch batch = batchService.createBatch(
                    product, batchNumber.trim(), quantity, LocalDate.now(), expiryDate,
                    costPerUnit, product.getSellingPrice(), supplier, "Quick scanned batch entry", user
            );

            response.put("success", true);
            response.put("message", "Batch " + batch.getBatchNumber() + " added successfully!");
            response.put("batchId", batch.getId());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/quick-register")
    @Transactional
    public ResponseEntity<Map<String, Object>> quickRegister(
            @RequestParam("barcode") String barcode,
            @RequestParam("name") String name,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("brand") String brand,
            @RequestParam(value = "unit", defaultValue = "pcs") String unit,
            @RequestParam(value = "minStockQuantity", defaultValue = "10") Integer minStockQuantity,
            @RequestParam(value = "purchasePrice", required = false) BigDecimal purchasePrice,
            @RequestParam(value = "sellingPrice", required = false) BigDecimal sellingPrice,
            @RequestParam("batchNumber") String batchNumber,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("expiryDate") String expiryDateStr,
            @RequestParam(value = "supplier", required = false) String supplier,
            @RequestParam(value = "notes", required = false) String notes,
            @AuthenticationPrincipal UserDetails userDetails) {

        Map<String, Object> response = new HashMap<>();
        try {
            if (productService.existsByBarcode(barcode.trim())) {
                throw new IllegalArgumentException("Barcode '" + barcode + "' is already registered in the system.");
            }

            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

            Product product = new Product(
                    name.trim(), category, brand.trim(), barcode.trim(), unit,
                    minStockQuantity != null ? minStockQuantity : 10,
                    purchasePrice != null ? purchasePrice : BigDecimal.ZERO,
                    sellingPrice != null ? sellingPrice : BigDecimal.ZERO
            );
            Product savedProduct = productService.saveProduct(product);

            LocalDate expiryDate = LocalDate.parse(expiryDateStr);
            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;

            ProductBatch batch = batchService.createBatch(
                    savedProduct, batchNumber.trim(), quantity, LocalDate.now(), expiryDate,
                    purchasePrice, sellingPrice, supplier, notes != null ? notes : "Scanned barcode initial registration", user
            );

            response.put("success", true);
            response.put("message", "Product '" + savedProduct.getName() + "' and Batch '" + batch.getBatchNumber() + "' stored in database successfully!");
            response.put("productId", savedProduct.getId());
            response.put("batchId", batch.getId());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            response.put("success", false);
            response.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
