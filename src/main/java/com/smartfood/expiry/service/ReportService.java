package com.smartfood.expiry.service;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.dto.ProductViewDto;
import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.WasteRecord;
import com.smartfood.expiry.enums.ExpiryStatus;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.ProductRepository;
import com.smartfood.expiry.repository.StockMovementRepository;
import com.smartfood.expiry.repository.WasteRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository movementRepository;
    private final WasteRecordRepository wasteRecordRepository;
    private final ExpiryDetectionService expiryDetectionService;
    private final ProductService productService;

    public ReportService(ProductRepository productRepository,
                         ProductBatchRepository batchRepository,
                         CategoryRepository categoryRepository,
                         StockMovementRepository movementRepository,
                         WasteRecordRepository wasteRecordRepository,
                         ExpiryDetectionService expiryDetectionService,
                         ProductService productService) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.categoryRepository = categoryRepository;
        this.movementRepository = movementRepository;
        this.wasteRecordRepository = wasteRecordRepository;
        this.expiryDetectionService = expiryDetectionService;
        this.productService = productService;
    }

    public List<BatchItemViewDto> getCurrentInventoryReport(LocalDate referenceDate) {
        return expiryDetectionService.getAllActiveBatchViews(referenceDate);
    }

    public List<BatchItemViewDto> getNearExpiryReport(LocalDate referenceDate) {
        return expiryDetectionService.getNearExpiryBatches(referenceDate);
    }

    public List<BatchItemViewDto> getExpiredStockReport(LocalDate referenceDate) {
        return expiryDetectionService.getExpiredBatches(referenceDate);
    }

    public List<ProductViewDto> getLowStockReport(LocalDate referenceDate) {
        return productService.getLowStockProducts(referenceDate);
    }

    public List<StockMovement> getBatchHistoryReport(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return movementRepository.findByMovementDateBetweenOrderByMovementDateDesc(
                    startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        }
        return movementRepository.findAllByOrderByMovementDateDesc();
    }

    public List<WasteRecord> getFoodWasteReport(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return wasteRecordRepository.findByWasteDateBetweenOrderByWasteDateDesc(startDate, endDate);
        }
        return wasteRecordRepository.findAllByOrderByWasteDateDesc();
    }

    public static class CategorySummaryDto {
        private String categoryName;
        private String categoryCode;
        private int productCount;
        private int totalStock;
        private int warningDays;
        private BigDecimal totalCostValue;

        public CategorySummaryDto(String categoryName, String categoryCode, int productCount, int totalStock, int warningDays, BigDecimal totalCostValue) {
            this.categoryName = categoryName;
            this.categoryCode = categoryCode;
            this.productCount = productCount;
            this.totalStock = totalStock;
            this.warningDays = warningDays;
            this.totalCostValue = totalCostValue;
        }

        public String getCategoryName() { return categoryName; }
        public String getCategoryCode() { return categoryCode; }
        public int getProductCount() { return productCount; }
        public int getTotalStock() { return totalStock; }
        public int getWarningDays() { return warningDays; }
        public BigDecimal getTotalCostValue() { return totalCostValue; }
    }

    public List<CategorySummaryDto> getCategoryWiseReport(LocalDate referenceDate) {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();
        List<CategorySummaryDto> summaries = new ArrayList<>();

        for (Category cat : categories) {
            List<Product> products = productRepository.findByCategoryAndActiveTrueOrderByNameAsc(cat);
            List<ProductBatch> batches = batchRepository.findByCategoryIdWithStock(cat.getId());

            int totalStock = batches.stream().mapToInt(ProductBatch::getQuantity).sum();
            BigDecimal costValue = batches.stream()
                    .map(ProductBatch::getTotalCostValue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            summaries.add(new CategorySummaryDto(
                    cat.getName(),
                    cat.getCode(),
                    products.size(),
                    totalStock,
                    cat.getDefaultWarningDays(),
                    costValue
            ));
        }

        return summaries;
    }

    public String generateCsv(String reportType, LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        StringBuilder csv = new StringBuilder();

        switch (reportType.toUpperCase()) {
            case "CURRENT_INVENTORY":
                csv.append("Batch Number,Product Name,Barcode,Category,Brand,Quantity,Unit,Purchase Date,Expiry Date,Days Remaining,Status,Cost Price,Selling Price,Supplier\n");
                for (BatchItemViewDto view : getCurrentInventoryReport(today)) {
                    ProductBatch b = view.getBatch();
                    csv.append(escapeCsv(b.getBatchNumber())).append(",")
                            .append(escapeCsv(b.getProduct().getName())).append(",")
                            .append(escapeCsv(b.getProduct().getBarcode())).append(",")
                            .append(escapeCsv(b.getProduct().getCategory().getName())).append(",")
                            .append(escapeCsv(b.getProduct().getBrand())).append(",")
                            .append(b.getQuantity()).append(",")
                            .append(escapeCsv(b.getProduct().getUnit())).append(",")
                            .append(b.getPurchaseDate()).append(",")
                            .append(b.getExpiryDate()).append(",")
                            .append(view.getDaysRemaining()).append(",")
                            .append(view.getStatusDisplayName()).append(",")
                            .append(b.getCostPerUnit()).append(",")
                            .append(b.getSellingPricePerUnit()).append(",")
                            .append(escapeCsv(b.getSupplier())).append("\n");
                }
                break;

            case "NEAR_EXPIRY":
                csv.append("Batch Number,Product Name,Barcode,Category,Quantity,Expiry Date,Days Remaining,Warning Threshold (Days),Status,Cost Value at Risk\n");
                for (BatchItemViewDto view : getNearExpiryReport(today)) {
                    ProductBatch b = view.getBatch();
                    csv.append(escapeCsv(b.getBatchNumber())).append(",")
                            .append(escapeCsv(b.getProduct().getName())).append(",")
                            .append(escapeCsv(b.getProduct().getBarcode())).append(",")
                            .append(escapeCsv(b.getProduct().getCategory().getName())).append(",")
                            .append(b.getQuantity()).append(",")
                            .append(b.getExpiryDate()).append(",")
                            .append(view.getDaysRemaining()).append(",")
                            .append(view.getWarningDaysUsed()).append(",")
                            .append(view.getStatusDisplayName()).append(",")
                            .append(view.getTotalCost()).append("\n");
                }
                break;

            case "EXPIRED":
                csv.append("Batch Number,Product Name,Barcode,Category,Quantity,Expiry Date,Days Expired,Estimated Cost Loss,Supplier\n");
                for (BatchItemViewDto view : getExpiredStockReport(today)) {
                    ProductBatch b = view.getBatch();
                    csv.append(escapeCsv(b.getBatchNumber())).append(",")
                            .append(escapeCsv(b.getProduct().getName())).append(",")
                            .append(escapeCsv(b.getProduct().getBarcode())).append(",")
                            .append(escapeCsv(b.getProduct().getCategory().getName())).append(",")
                            .append(b.getQuantity()).append(",")
                            .append(b.getExpiryDate()).append(",")
                            .append(Math.abs(view.getDaysRemaining())).append(",")
                            .append(view.getTotalCost()).append(",")
                            .append(escapeCsv(b.getSupplier())).append("\n");
                }
                break;

            case "LOW_STOCK":
                csv.append("Product Name,Barcode,Category,Brand,Current Valid Stock,Min Stock Threshold,Unit,Status\n");
                for (ProductViewDto view : getLowStockReport(today)) {
                    csv.append(escapeCsv(view.getProduct().getName())).append(",")
                            .append(escapeCsv(view.getProduct().getBarcode())).append(",")
                            .append(escapeCsv(view.getProduct().getCategory().getName())).append(",")
                            .append(escapeCsv(view.getProduct().getBrand())).append(",")
                            .append(view.getTotalValidStock()).append(",")
                            .append(view.getProduct().getMinStockQuantity()).append(",")
                            .append(escapeCsv(view.getProduct().getUnit())).append(",")
                            .append(view.isOutOfStock() ? "OUT OF STOCK" : "LOW STOCK").append("\n");
                }
                break;

            case "WASTE":
                csv.append("Waste Date,Product Name,Batch Number,Barcode,Quantity Discarded,Reason,Estimated Financial Loss,Recorded By,Notes\n");
                for (WasteRecord w : getFoodWasteReport(startDate, endDate)) {
                    csv.append(w.getWasteDate()).append(",")
                            .append(escapeCsv(w.getBatch().getProduct().getName())).append(",")
                            .append(escapeCsv(w.getBatch().getBatchNumber())).append(",")
                            .append(escapeCsv(w.getBatch().getProduct().getBarcode())).append(",")
                            .append(w.getQuantity()).append(",")
                            .append(escapeCsv(w.getWasteReason().getLabel())).append(",")
                            .append(w.getEstimatedCostLoss()).append(",")
                            .append(escapeCsv(w.getRecordedByUser() != null ? w.getRecordedByUser().getFullName() : "N/A")).append(",")
                            .append(escapeCsv(w.getNotes())).append("\n");
                }
                break;

            case "CATEGORY":
                csv.append("Category Name,Code,Products Count,Total Stock Quantity,Warning Days,Total Inventory Value\n");
                for (CategorySummaryDto cat : getCategoryWiseReport(today)) {
                    csv.append(escapeCsv(cat.getCategoryName())).append(",")
                            .append(escapeCsv(cat.getCategoryCode())).append(",")
                            .append(cat.getProductCount()).append(",")
                            .append(cat.getTotalStock()).append(",")
                            .append(cat.getWarningDays()).append(",")
                            .append(cat.getTotalCostValue()).append("\n");
                }
                break;

            default:
                csv.append("ID,Batch,Movement Type,Quantity,Previous Qty,New Qty,Date,Reason,User,Notes\n");
                for (StockMovement m : getBatchHistoryReport(startDate, endDate)) {
                    csv.append(m.getId()).append(",")
                            .append(escapeCsv(m.getBatch().getBatchNumber())).append(",")
                            .append(m.getMovementType().name()).append(",")
                            .append(m.getQuantity()).append(",")
                            .append(m.getPreviousQuantity()).append(",")
                            .append(m.getNewQuantity()).append(",")
                            .append(m.getMovementDate()).append(",")
                            .append(escapeCsv(m.getReason())).append(",")
                            .append(escapeCsv(m.getPerformedByUser() != null ? m.getPerformedByUser().getFullName() : "N/A")).append(",")
                            .append(escapeCsv(m.getNotes())).append("\n");
                }
                break;
        }

        return csv.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        String s = val.replace("\"", "\"\"");
        String trimmed = s.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
