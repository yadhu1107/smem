package com.smartfood.expiry.config;

import com.smartfood.expiry.entity.Category;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.entity.StockMovement;
import com.smartfood.expiry.entity.User;
import com.smartfood.expiry.enums.MovementType;
import com.smartfood.expiry.enums.RoleType;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.repository.ProductBatchRepository;
import com.smartfood.expiry.repository.ProductRepository;
import com.smartfood.expiry.repository.StockMovementRepository;
import com.smartfood.expiry.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           ProductBatchRepository batchRepository,
                           StockMovementRepository movementRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.movementRepository = movementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initUsers();
        initCategories();
        initDemoProductsAndBatches();
    }

    private void initUsers() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User(
                    "admin",
                    "admin@smartfood.com",
                    passwordEncoder.encode("admin123"),
                    "System Administrator",
                    RoleType.ROLE_ADMIN
            );
            userRepository.save(admin);
            log.info("Default ADMIN user initialized: admin / admin123");
        }

        if (!userRepository.existsByUsername("staff")) {
            User staff = new User(
                    "staff",
                    "staff@smartfood.com",
                    passwordEncoder.encode("staff123"),
                    "Store Staff Member",
                    RoleType.ROLE_STAFF
            );
            userRepository.save(staff);
            log.info("Default STAFF user initialized: staff / staff123");
        }
    }

    private void initCategories() {
        createCategoryIfAbsent("Dairy & Eggs", "DAIRY", 3, "Milk, yogurt, cheese, butter, eggs");
        createCategoryIfAbsent("Bakery", "BAKERY", 2, "Fresh bread, pastries, buns, cakes");
        createCategoryIfAbsent("Fresh Produce", "PRODUCE", 3, "Fresh vegetables, fruits, herbs");
        createCategoryIfAbsent("Meat & Poultry", "MEAT", 2, "Fresh chicken, beef, seafood");
        createCategoryIfAbsent("Canned Goods", "CANNED", 30, "Preserved vegetables, beans, canned fish");
        createCategoryIfAbsent("Beverages", "BEVERAGES", 15, "Juices, sodas, packaged drinks");
        createCategoryIfAbsent("Frozen Foods", "FROZEN", 14, "Ice creams, frozen meals, frozen veggies");
        createCategoryIfAbsent("Snacks & Dry Goods", "SNACKS", 10, "Chips, biscuits, cereal, pasta");
    }

    private void createCategoryIfAbsent(String name, String code, int warningDays, String desc) {
        if (categoryRepository.findByCode(code).isEmpty()) {
            categoryRepository.save(new Category(name, code, warningDays, desc));
        }
    }

    private void initDemoProductsAndBatches() {
        Category dairy = categoryRepository.findByCode("DAIRY").orElse(null);
        Category bakery = categoryRepository.findByCode("BAKERY").orElse(null);
        Category meat = categoryRepository.findByCode("MEAT").orElse(null);
        Category produce = categoryRepository.findByCode("PRODUCE").orElse(null);
        Category canned = categoryRepository.findByCode("CANNED").orElse(null);
        Category beverages = categoryRepository.findByCode("BEVERAGES").orElse(null);
        Category frozen = categoryRepository.findByCode("FROZEN").orElse(null);
        Category snacks = categoryRepository.findByCode("SNACKS").orElse(null);

        User admin = userRepository.findByUsername("admin").orElse(null);
        LocalDate today = LocalDate.now();

        // 1. Whole Milk 1L (Dairy)
        seedProduct(admin, "890126201001", "Farm Fresh Whole Milk 1L", dairy, "Amul / Meadow Gold", "liters", 15,
                new BigDecimal("42.00"), new BigDecimal("54.00"),
                new BatchSeed("BAT-MILK-001", 8, 30, today.minusDays(10), today.minusDays(2), new BigDecimal("42.00"), "Dairy Coop Ltd"), // Expired
                new BatchSeed("BAT-MILK-002", 18, 25, today.minusDays(4), today.plusDays(2), new BigDecimal("42.00"), "Dairy Coop Ltd"),  // Near Expiry
                new BatchSeed("BAT-MILK-003", 40, 40, today.minusDays(1), today.plusDays(7), new BigDecimal("42.00"), "Dairy Coop Ltd")   // Safe
        );

        // 2. Whole Wheat Brown Bread (Bakery)
        seedProduct(admin, "890126202002", "Whole Wheat Brown Bread 400g", bakery, "Modern Harvest", "pack", 12,
                new BigDecimal("28.00"), new BigDecimal("40.00"),
                new BatchSeed("BAT-BRD-101", 6, 20, today.minusDays(3), today, new BigDecimal("28.00"), "Sunrise Bakeries"),               // Expires Today
                new BatchSeed("BAT-BRD-102", 22, 30, today.minusDays(1), today.plusDays(3), new BigDecimal("28.00"), "Sunrise Bakeries")  // Safe
        );

        // 3. Greek Style Natural Yogurt (Dairy)
        seedProduct(admin, "890126203003", "Greek Style Natural Yogurt 500g", dairy, "Epigamia", "pcs", 10,
                new BigDecimal("65.00"), new BigDecimal("95.00"),
                new BatchSeed("BAT-YOG-201", 12, 20, today.minusDays(12), today.plusDays(1), new BigDecimal("65.00"), "Cold Chain Logistics"), // Near Expiry
                new BatchSeed("BAT-YOG-202", 30, 30, today.minusDays(2), today.plusDays(14), new BigDecimal("65.00"), "Cold Chain Logistics")  // Safe
        );

        // 4. Prime Fresh Chicken Breast (Meat - Low Stock)
        seedProduct(admin, "890126204004", "Prime Fresh Chicken Breast 500g", meat, "FreshToHome", "pack", 15,
                new BigDecimal("140.00"), new BigDecimal("195.00"),
                new BatchSeed("BAT-CHK-301", 5, 15, today.minusDays(1), today.plusDays(2), new BigDecimal("140.00"), "Royal Farms") // Near Expiry & Low Stock
        );

        // 5. Organic Strawberries (Produce)
        seedProduct(admin, "890126205005", "Organic Fresh Strawberries 250g", produce, "Berry Valley", "pack", 8,
                new BigDecimal("80.00"), new BigDecimal("130.00"),
                new BatchSeed("BAT-STR-401", 4, 15, today.minusDays(5), today.minusDays(1), new BigDecimal("80.00"), "Highland Orchards"), // Expired
                new BatchSeed("BAT-STR-402", 14, 20, today.minusDays(1), today.plusDays(4), new BigDecimal("80.00"), "Highland Orchards")  // Safe
        );

        // 6. Sweet Kernel Corn (Canned)
        seedProduct(admin, "890126206006", "Golden Sweet Kernel Corn 400g", canned, "Del Monte", "pcs", 20,
                new BigDecimal("55.00"), new BigDecimal("85.00"),
                new BatchSeed("BAT-CRN-501", 45, 50, today.minusDays(60), today.plusDays(180), new BigDecimal("55.00"), "Global Agro Foods")
        );

        // 7. Orange Juice 1L (Beverages)
        seedProduct(admin, "890126207007", "100% Squeezed Orange Juice 1L", beverages, "Tropicana", "liters", 10,
                new BigDecimal("70.00"), new BigDecimal("110.00"),
                new BatchSeed("BAT-ORJ-601", 7, 20, today.minusDays(15), today.plusDays(5), new BigDecimal("70.00"), "Sunrise Beverages") // Near Expiry
        );

        // 8. Salted Butter 500g (Dairy)
        seedProduct(admin, "890126208008", "Pure Creamery Salted Butter 500g", dairy, "Kerrygold", "pcs", 12,
                new BigDecimal("110.00"), new BigDecimal("165.00"),
                new BatchSeed("BAT-BTR-701", 15, 25, today.minusDays(20), today.plusDays(2), new BigDecimal("110.00"), "Irish Dairy Direct"), // Near Expiry
                new BatchSeed("BAT-BTR-702", 35, 40, today.minusDays(5), today.plusDays(45), new BigDecimal("110.00"), "Irish Dairy Direct")  // Safe
        );

        // 9. Multigrain Sourdough Bread (Bakery)
        seedProduct(admin, "890126209009", "Artisan Multigrain Sourdough Bread 450g", bakery, "The French Baker", "pack", 8,
                new BigDecimal("45.00"), new BigDecimal("75.00"),
                new BatchSeed("BAT-SOU-801", 9, 15, today.minusDays(1), today.plusDays(1), new BigDecimal("45.00"), "French Bakeries Ltd") // Near Expiry
        );

        // 10. Fresh Atlantic Salmon Fillet (Meat & Seafood)
        seedProduct(admin, "890126210010", "Fresh Atlantic Salmon Fillet 300g", meat, "Ocean Catch", "pack", 6,
                new BigDecimal("220.00"), new BigDecimal("320.00"),
                new BatchSeed("BAT-SLM-901", 4, 10, today.minusDays(3), today.minusDays(1), new BigDecimal("220.00"), "Nordic Seafoods"), // Expired
                new BatchSeed("BAT-SLM-902", 8, 12, today.minusDays(1), today.plusDays(2), new BigDecimal("220.00"), "Nordic Seafoods")   // Near Expiry
        );

        // 11. Hass Avocados Pack of 4 (Produce)
        seedProduct(admin, "890126211011", "Premium Hass Avocados 4-Pack", produce, "Nature's Basket", "pack", 10,
                new BigDecimal("120.00"), new BigDecimal("180.00"),
                new BatchSeed("BAT-AVO-101", 16, 20, today.minusDays(2), today.plusDays(3), new BigDecimal("120.00"), "Peruvian Growers") // Near Expiry
        );

        // 12. Organic Baby Spinach (Produce)
        seedProduct(admin, "890126212012", "Organic Tender Baby Spinach 200g", produce, "Green Fields", "pack", 12,
                new BigDecimal("35.00"), new BigDecimal("60.00"),
                new BatchSeed("BAT-SPN-111", 5, 18, today.minusDays(4), today, new BigDecimal("35.00"), "Green Fields Organic"),         // Expires Today
                new BatchSeed("BAT-SPN-112", 20, 25, today.minusDays(1), today.plusDays(4), new BigDecimal("35.00"), "Green Fields Organic") // Safe
        );

        // 13. Crushed Italian Tomatoes Can (Canned)
        seedProduct(admin, "890126213013", "Crushed Italian San Marzano Tomatoes 800g", canned, "Mutti", "pcs", 15,
                new BigDecimal("85.00"), new BigDecimal("135.00"),
                new BatchSeed("BAT-TOM-121", 50, 60, today.minusDays(90), today.plusDays(365), new BigDecimal("85.00"), "Parma Imports") // Safe
        );

        // 14. Unsweetened Almond Milk (Beverages)
        seedProduct(admin, "890126214014", "Unsweetened Vanilla Almond Milk 1L", beverages, "Silk Organics", "liters", 10,
                new BigDecimal("95.00"), new BigDecimal("150.00"),
                new BatchSeed("BAT-ALM-131", 6, 20, today.minusDays(30), today.plusDays(8), new BigDecimal("95.00"), "Plant Milks Corp") // Near Expiry (15d warning)
        );

        // 15. Aged English Cheddar (Dairy)
        seedProduct(admin, "890126215015", "Mature English White Cheddar 200g", dairy, "Cathedral City", "pcs", 14,
                new BigDecimal("130.00"), new BigDecimal("190.00"),
                new BatchSeed("BAT-CHD-141", 11, 20, today.minusDays(15), today.plusDays(2), new BigDecimal("130.00"), "British Dairy Exports"), // Near Expiry
                new BatchSeed("BAT-CHD-142", 28, 30, today.minusDays(5), today.plusDays(60), new BigDecimal("130.00"), "British Dairy Exports")  // Safe
        );

        // 16. Wild Frozen Blueberries (Frozen)
        seedProduct(admin, "890126216016", "Wild Maine Frozen Blueberries 500g", frozen, "Wyman's", "pack", 10,
                new BigDecimal("115.00"), new BigDecimal("175.00"),
                new BatchSeed("BAT-BLU-151", 8, 20, today.minusDays(120), today.plusDays(10), new BigDecimal("115.00"), "Arctic Cold Logistics"), // Near Expiry (14d warning)
                new BatchSeed("BAT-BLU-152", 30, 30, today.minusDays(10), today.plusDays(180), new BigDecimal("115.00"), "Arctic Cold Logistics") // Safe
        );

        // 17. Belgian Chocolate Ice Cream (Frozen)
        seedProduct(admin, "890126217017", "Belgian Dark Chocolate Ice Cream 1L", frozen, "Häagen-Dazs", "pcs", 12,
                new BigDecimal("180.00"), new BigDecimal("270.00"),
                new BatchSeed("BAT-ICE-161", 24, 30, today.minusDays(20), today.plusDays(120), new BigDecimal("180.00"), "Global Confections")
        );

        // 18. Crunchy Almond Granola (Snacks & Dry Goods)
        seedProduct(admin, "890126218018", "Crunchy Honey Roasted Almond Granola 400g", snacks, "Kellogg's", "pack", 15,
                new BigDecimal("75.00"), new BigDecimal("120.00"),
                new BatchSeed("BAT-GRN-171", 12, 25, today.minusDays(45), today.plusDays(8), new BigDecimal("75.00"), "Cereal Mills Ltd"), // Near Expiry (10d warning)
                new BatchSeed("BAT-GRN-172", 40, 50, today.minusDays(10), today.plusDays(90), new BigDecimal("75.00"), "Cereal Mills Ltd") // Safe
        );

        // 19. Extra Virgin Olive Oil (Canned / Pantry)
        seedProduct(admin, "890126219019", "Cold Extracted Extra Virgin Olive Oil 500ml", canned, "Borges", "bottles", 8,
                new BigDecimal("160.00"), new BigDecimal("240.00"),
                new BatchSeed("BAT-OIL-181", 18, 24, today.minusDays(90), today.plusDays(240), new BigDecimal("160.00"), "Mediterranean Imports")
        );

        // 20. French Butter Croissants (Bakery)
        seedProduct(admin, "890126220020", "Traditional French Butter Croissants 4-Pack", bakery, "Saint Honoré", "pack", 10,
                new BigDecimal("50.00"), new BigDecimal("85.00"),
                new BatchSeed("BAT-CRO-191", 3, 15, today.minusDays(4), today.minusDays(1), new BigDecimal("50.00"), "Artisan Boulangerie"), // Expired
                new BatchSeed("BAT-CRO-192", 14, 20, today.minusDays(1), today.plusDays(2), new BigDecimal("50.00"), "Artisan Boulangerie")   // Near Expiry
        );

        log.info("Initialized full 20 demonstration barcode products and multi-batch inventory scenarios.");
    }

    private static class BatchSeed {
        String batchNumber;
        int qty;
        int initialQty;
        LocalDate purchaseDate;
        LocalDate expiryDate;
        BigDecimal cost;
        String supplier;

        BatchSeed(String batchNumber, int qty, int initialQty, LocalDate purchaseDate, LocalDate expiryDate, BigDecimal cost, String supplier) {
            this.batchNumber = batchNumber;
            this.qty = qty;
            this.initialQty = initialQty;
            this.purchaseDate = purchaseDate;
            this.expiryDate = expiryDate;
            this.cost = cost;
            this.supplier = supplier;
        }
    }

    private void seedProduct(User admin, String barcode, String name, Category category, String brand,
                             String unit, int minStock, BigDecimal cost, BigDecimal price,
                             BatchSeed... batches) {
        Product product = productRepository.findByBarcode(barcode).orElse(null);
        if (product == null) {
            product = new Product(name, category, brand, barcode, unit, minStock, cost, price);
            product = productRepository.save(product);
        }

        for (BatchSeed b : batches) {
            if (batchRepository.findByProductAndBatchNumber(product, b.batchNumber).isEmpty()) {
                ProductBatch batch = new ProductBatch(
                        product, b.batchNumber, b.qty, b.initialQty,
                        b.purchaseDate, b.expiryDate, b.cost, product.getSellingPrice(), b.supplier
                );
                batch = batchRepository.save(batch);

                StockMovement inward = new StockMovement(
                        batch, MovementType.PURCHASE_IN, b.initialQty, 0, b.initialQty,
                        "Initial stock inward via batch registration", admin, "Batch " + b.batchNumber
                );
                movementRepository.save(inward);

                if (b.initialQty > b.qty) {
                    int sold = b.initialQty - b.qty;
                    StockMovement sale = new StockMovement(
                            batch, MovementType.SALE_OUT, sold, b.initialQty, b.qty,
                            "Customer POS checkout", admin, "Sales reduction"
                    );
                    movementRepository.save(sale);
                }
            }
        }
    }
}
