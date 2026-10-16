package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.MovementType;
import com.smartfood.expiry.service.BatchService;
import com.smartfood.expiry.service.ExpiryDetectionService;
import com.smartfood.expiry.service.ProductService;
import com.smartfood.expiry.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/batches")
public class BatchController {

    private final BatchService batchService;
    private final ProductService productService;
    private final ExpiryDetectionService expiryDetectionService;
    private final UserService userService;

    public BatchController(BatchService batchService,
                           ProductService productService,
                           ExpiryDetectionService expiryDetectionService,
                           UserService userService) {
        this.batchService = batchService;
        this.productService = productService;
        this.expiryDetectionService = expiryDetectionService;
        this.userService = userService;
    }

    @GetMapping
    public String listBatches(@RequestParam(value = "statusFilter", required = false) String statusFilter,
                              Model model) {
        LocalDate today = LocalDate.now();
        List<BatchItemViewDto> allViews = expiryDetectionService.getAllActiveBatchViews(today);

        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            allViews = allViews.stream()
                    .filter(v -> v.getStatus().name().equalsIgnoreCase(statusFilter.trim()))
                    .collect(Collectors.toList());
        }

        model.addAttribute("batchViews", allViews);
        model.addAttribute("selectedStatusFilter", statusFilter);
        model.addAttribute("activeNav", "batches");

        return "batches/list";
    }

    @GetMapping("/new")
    public String newBatchForm(@RequestParam(value = "productId", required = false) Long productId,
                               @RequestParam(value = "barcode", required = false) String barcode,
                               Model model) {
        Product selectedProduct = null;
        if (productId != null) {
            selectedProduct = productService.getProductById(productId).orElse(null);
        } else if (barcode != null && !barcode.trim().isEmpty()) {
            selectedProduct = productService.getProductByBarcode(barcode.trim()).orElse(null);
        }

        model.addAttribute("products", productService.getAllActiveProducts());
        model.addAttribute("selectedProduct", selectedProduct);
        model.addAttribute("defaultPurchaseDate", LocalDate.now());
        model.addAttribute("activeNav", "batches");

        return "batches/form";
    }

    @PostMapping("/save")
    public String saveBatch(@RequestParam("productId") Long productId,
                            @RequestParam("batchNumber") String batchNumber,
                            @RequestParam("quantity") Integer quantity,
                            @RequestParam("purchaseDate") String purchaseDateStr,
                            @RequestParam("expiryDate") String expiryDateStr,
                            @RequestParam(value = "costPerUnit", required = false) BigDecimal costPerUnit,
                            @RequestParam(value = "sellingPricePerUnit", required = false) BigDecimal sellingPricePerUnit,
                            @RequestParam(value = "supplier", required = false) String supplier,
                            @RequestParam(value = "notes", required = false) String notes,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.getProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + productId));

            LocalDate purchaseDate = (purchaseDateStr != null && !purchaseDateStr.isEmpty())
                    ? LocalDate.parse(purchaseDateStr) : LocalDate.now();
            LocalDate expiryDate = LocalDate.parse(expiryDateStr);

            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;

            ProductBatch batch = batchService.createBatch(
                    product, batchNumber.trim(), quantity, purchaseDate, expiryDate,
                    costPerUnit, sellingPricePerUnit, supplier, notes, user
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Batch '" + batch.getBatchNumber() + "' created with " + quantity + " units of " + product.getName() + ".");

            return "redirect:/products/view/" + product.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving batch: " + ex.getMessage());
            return "redirect:/batches/new" + (productId != null ? "?productId=" + productId : "");
        }
    }

    @PostMapping("/adjust")
    public String adjustStock(@RequestParam("batchId") Long batchId,
                              @RequestParam("adjustmentType") String adjustmentType,
                              @RequestParam("quantity") Integer quantity,
                              @RequestParam("reason") String reason,
                              @RequestParam(value = "notes", required = false) String notes,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            int change = "IN".equalsIgnoreCase(adjustmentType) ? quantity : -quantity;
            MovementType movementType = "IN".equalsIgnoreCase(adjustmentType) ? MovementType.ADJUSTMENT_IN : MovementType.ADJUSTMENT_OUT;

            User user = (userDetails != null) ? userService.getUserByUsername(userDetails.getUsername()).orElse(null) : null;

            ProductBatch updated = batchService.adjustStock(batchId, change, movementType, reason, notes, user);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Batch " + updated.getBatchNumber() + " stock updated. New stock: " + updated.getQuantity() + " units.");

            return "redirect:/batches/history/" + batchId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to adjust stock: " + ex.getMessage());
            return "redirect:/batches";
        }
    }

    @GetMapping("/history/{id}")
    public String batchHistory(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return batchService.getBatchById(id).map(batch -> {
            LocalDate today = LocalDate.now();
            BatchItemViewDto batchView = expiryDetectionService.wrapBatch(batch, today);
            List<StockMovement> movements = batchService.getMovementsForBatch(batch);

            model.addAttribute("batchView", batchView);
            model.addAttribute("movements", movements);
            model.addAttribute("activeNav", "batches");

            return "batches/history";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Batch not found with ID: " + id);
            return "redirect:/batches";
        });
    }
}
