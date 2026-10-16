# Smart Food Expiry Management System

A full-stack, enterprise-grade inventory and food waste management solution engineered for supermarkets, grocery stores, and food retailers.

Built with **Java 17**, **Spring Boot 3.2**, **Spring Security 6**, **Spring Data JPA**, **Thymeleaf**, **Bootstrap 5**, and **H2/MySQL**.

---

## Key Features & Business Modules

### 1. Modern Dashboard (White & Green Theme)
- **6 Real-Time KPI Cards**: Total Products, Total Shelf Stock, Near-Expiry Warnings, Expired Stock, Low-Stock Alerts, and **Stock Value at Risk ($)**.
- **Dynamic Interactive Visualizations** (powered by Chart.js):
  - *Expiry Status Breakdown* (Safe vs. Near-Expiry vs. Expired).
  - *Category Inventory Distribution*.
  - *Upcoming Expiration Timeline* (1-3 days, 4-7 days, 8-14 days, 15-30 days, 30+ days).
- **Critical Items Table**: Immediate visual status badges with direct waste disposal or surplus donation actions.

### 2. Product Catalog & Barcode Scanning
- Comprehensive product registry with brand, category, unit of measure, minimum stock threshold, and purchase/selling prices.
- **Hardware & Camera Barcode Support**:
  - **USB Hardware Scanner**: Global low-latency listener automatically detects barcode scans without requiring manual input focus.
  - **Camera Scanner**: Integrated device camera scanning powered by `html5-qrcode`.
  - **Automatic Database Lookup**: Instantly verifies registered products, shows existing active batches, and offers 1-click batch additions or new product registration.

### 3. Batch-Wise Inventory & Audit Movements
- Supports multiple discrete batches per product, each tracking its own **Batch Number**, **Quantity**, **Manufacture / Purchase Date**, **Expiry Date**, and **Supplier**.
- **Tamper-Evident Stock Movements**: Every stock inward, outward sale, audit adjustment, waste disposal, or donation generates an immutable `StockMovement` audit record.
- **Negative Stock Guards**: Prevents invalid quantity adjustments or negative inventory states.

### 4. Dynamic Expiry Detection Engine
- Calculates days remaining dynamically using `java.time.LocalDate` rather than relying on stale stored flags.
- **Configurable Warning Thresholds**:
  - Global default warning period (e.g. 7 days).
  - **Category-specific overrides** (e.g., Dairy: 3 days, Bakery: 2 days, Meat: 2 days, Canned Goods: 30 days).
- Three standard operational statuses:
  - <span style="color: #15803d; font-weight: bold;">SAFE</span>: Expiry date is beyond the category warning period.
  - <span style="color: #b45309; font-weight: bold;">NEAR EXPIRY</span>: Expiry falls within the warning horizon or expires today.
  - <span style="color: #b91c1c; font-weight: bold;">EXPIRED</span>: Product is past expiration date (prohibited from sale or donation).

### 5. FEFO (First Expired, First Out) Stock Rotation
- Automatically prioritizes batches ordered strictly by earliest expiry date.
- Designates shelf rotation positions (*Front of Shelf Urgent*, *Front Shelf Rotation*, *Standard Display*).
- **Food Safety Assurance**: Expired batches are strictly filtered out and never recommended for sale.
- Print-friendly layout for store staff daily shelf replenishment.

### 6. Food Waste & Loss Management
- Record discarded items with reason classification (*Expired*, *Spoiled*, *Physical Damage*, *Packaging Defect*, *Cold Chain Failure*).
- Automatically calculates estimated financial loss based on unit cost.
- Cumulative shrinkage statistics and audit trail.

### 7. Surplus Food Donation Tracking
- Redirect safe, wholesome food nearing expiry to local charities, food banks, and shelters.
- Strict validation: **Expired products cannot be donated**.

### 8. Analytics & CSV Reporting
- 7 Built-in operational reports:
  1. *Current Inventory Report*
  2. *Near-Expiry Risk Report*
  3. *Expired Stock Report*
  4. *Low-Stock Reorder Report*
  5. *Batch Movement History Audit*
  6. *Food Waste & Loss Report*
  7. *Category-Wise Summary Report*
- Instant **CSV Export** for all reports.
- Print-ready stylesheet (`@media print`) that automatically formats clean documents without navigation clutter.
- Scheduled daily automated audit (`@Scheduled`) with optional SMTP email notifications.

### 9. Secure Role-Based Access Control (RBAC)
- Password hashing with **BCrypt**.
- Role separation:
  - **Store Administrator (`ROLE_ADMIN`)**: Complete system access, reports, analytics, and user account management.
  - **Store Staff (`ROLE_STAFF`)**: Product management, batch inwarding, expiry alerts, FEFO rotation, waste logging, and donations.

---

## Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.2.10 |
| **Security** | Spring Security 6 with BCrypt |
| **Persistence** | Spring Data JPA / Hibernate 6 |
| **Database** | Persistent H2 file (default local run) / MySQL 8.0+ (`mysql` profile) |
| **View Engine** | Thymeleaf + Spring Security Extras |
| **Frontend UI** | HTML5, CSS3, JavaScript, Bootstrap 5.3, Bootstrap Icons |
| **Charts** | Chart.js 4.4 |
| **Barcode Scanner** | USB Keyboard Emulation & `html5-qrcode` Camera Integration |
| **Build Tool** | Apache Maven 3.8+ |

---

## Default User Accounts

When the application starts for the first time, demo accounts are automatically provisioned:

| Role | Username | Password | Email | Permissions |
|---|---|---|---|---|
| **Administrator** | `admin` | `admin123` | `admin@smartfood.com` | Full store access, User management, Reports & CSV export |
| **Staff Member** | `staff` | `staff123` | `staff@smartfood.com` | Products, Batches, Expiry Alerts, FEFO, Waste, Donations |

*Note: You can also log in using the email address instead of the username.*
These are local demo credentials. Change them before using the application with real inventory or users.

The 20 internal demo barcode values are in `demo-barcodes.csv`; the signed-in **20 Demo Barcodes** page renders a printable Code 128 sheet from the database.

---

## Installation & Setup Instructions

### Prerequisites
1. **Java Development Kit (JDK) 17** or later installed.
2. **Apache Maven 3.8+** installed.
3. **MySQL Server 8.0+** is only needed when using the optional `mysql` profile. By default, the app uses a persistent embedded H2 database and starts without a separate database service.

The default H2 database is stored under `data/food_expiry_db` and survives application restarts. Keep this folder when you want to retain your local inventory.

---

### Step 1: Database Setup (Optional for MySQL)
1. Open MySQL Workbench or your command terminal.
2. Execute the setup script:
```sql
CREATE DATABASE IF NOT EXISTS food_expiry_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```
*(Alternatively, execute the provided `setup-db.sql` file).*

---

### Step 2: Configure Database Credentials (Optional for MySQL)
Set MySQL credentials in the terminal, then activate the `mysql` profile. The profile configuration is in `src/main/resources/application-mysql.properties`:
```properties
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
```

> **Tip**: You can also provide credentials via environment variables without editing the file:
> ```powershell
> $env:DB_USERNAME="root"
> $env:DB_PASSWORD="your_password"
> ```

---

### Step 3: Build & Run in VS Code / PowerShell on Windows

Open a PowerShell terminal in the project root directory (`smem`):

#### 1. Compile and Run Test Suite
```powershell
mvn clean test
```
All unit tests and integration tests will execute using the embedded in-memory test configuration.

#### 2. Launch the Application (persistent H2 by default)
```powershell
mvn spring-boot:run
```

To use MySQL instead, set your credentials and activate the profile:
```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Email expiry alerts are disabled by default. To enable SMTP delivery, configure `MAIL_USERNAME`, `MAIL_PASSWORD`, and `ALERT_EMAIL_TO`, then set `EMAIL_ALERTS_ENABLED=true` in the environment. Use a provider app password where required; do not commit mail credentials.

#### 3. Open in Browser
Once started, navigate to:
```text
http://localhost:8080
```
Log in using `admin` / `admin123` or `staff` / `staff123`.

---

## Running in Visual Studio Code

1. Open VS Code: `code .`
2. Install recommended extensions:
   - **Extension Pack for Java** (Microsoft)
   - **Spring Boot Extension Pack** (VMware)
3. Open the **Run and Debug** panel and choose **Run Smart Food Expiry System**.
4. Press `F5`. VS Code starts Spring Boot and opens Chrome after Tomcat is ready. The separate **Open Smart Food Expiry in Chrome** configuration only opens a browser for an already-running app.

---

## Troubleshooting Guide

| Issue | Cause | Solution |
|---|---|---|
| `Access denied for user 'root'@'localhost'` | MySQL password mismatch | Set `DB_PASSWORD` correctly and ensure the `mysql` profile is active. |
| `Communications link failure` | MySQL service is stopped | Start the Windows service: `Start-Service MySQL80` in PowerShell as Administrator. |
| `Port 8080 already in use` | Another process is listening on 8080 | Change `server.port=8081` in `application.properties` or kill the conflicting process. |
| Barcode scanner camera error | Camera permissions not granted in browser | Click "Allow camera access" in your browser bar, or use the USB keyboard barcode input field. |
| Tests fail with SQL syntax error | Schema conflict during H2 tests | Ensure `spring.sql.init.mode=never` is maintained in `application-test.properties`. |

---

## Project Structure Overview

```text
smem/
├── pom.xml
├── setup-db.sql
├── README.md
└── src/
    ├── main/
    │   ├── java/com/smartfood/expiry/
    │   │   ├── SmartFoodExpiryApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   └── DataInitializer.java
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── ProductController.java
    │   │   │   ├── BatchController.java
    │   │   │   ├── ExpiryAlertController.java
    │   │   │   ├── WasteController.java
    │   │   │   ├── DonationController.java
    │   │   │   ├── ReportController.java
    │   │   │   ├── UserController.java
    │   │   │   └── BarcodeApiController.java
    │   │   ├── dto/
    │   │   │   ├── DashboardStatsDto.java
    │   │   │   ├── BatchItemViewDto.java
    │   │   │   ├── ProductViewDto.java
    │   │   │   └── BarcodeLookupDto.java
    │   │   ├── entity/
    │   │   │   ├── User.java
    │   │   │   ├── Category.java
    │   │   │   ├── Product.java
    │   │   │   ├── ProductBatch.java
    │   │   │   ├── StockMovement.java
    │   │   │   ├── ExpiryNotification.java
    │   │   │   ├── WasteRecord.java
    │   │   │   └── DonationRecord.java
    │   │   ├── enums/
    │   │   │   ├── RoleType.java
    │   │   │   ├── ExpiryStatus.java
    │   │   │   ├── MovementType.java
    │   │   │   ├── WasteReason.java
    │   │   │   └── DonationStatus.java
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── CategoryRepository.java
    │   │   │   ├── ProductRepository.java
    │   │   │   ├── ProductBatchRepository.java
    │   │   │   ├── StockMovementRepository.java
    │   │   │   ├── ExpiryNotificationRepository.java
    │   │   │   ├── WasteRecordRepository.java
    │   │   │   └── DonationRecordRepository.java
    │   │   └── service/
    │   │       ├── CustomUserDetailsService.java
    │   │       ├── UserService.java
    │   │       ├── ProductService.java
    │   │       ├── BatchService.java
    │   │       ├── ExpiryDetectionService.java
    │   │       ├── FefoService.java
    │   │       ├── WasteService.java
    │   │       ├── DonationService.java
    │   │       ├── BarcodeService.java
    │   │       ├── NotificationService.java
    │   │       ├── ReportService.java
    │   │       └── DashboardService.java
    │   └── resources/
    │       ├── application.properties
    │       ├── schema.sql
    │       ├── static/
    │       │   ├── css/styles.css
    │       │   └── js/app.js
    │       └── templates/
    │           ├── fragments/
    │           │   ├── head.html
    │           │   ├── sidebar.html
    │           │   ├── navbar.html
    │           │   ├── alerts.html
    │           │   ├── barcode-modal.html
    │           │   └── footer.html
    │           ├── auth/login.html
    │           ├── dashboard/index.html
    │           ├── products/
    │           │   ├── list.html
    │           │   ├── form.html
    │           │   └── detail.html
    │           ├── batches/
    │           │   ├── list.html
    │           │   ├── form.html
    │           │   └── history.html
    │           ├── expiry/
    │           │   ├── alerts.html
    │           │   └── fefo.html
    │           ├── waste/
    │           │   ├── list.html
    │           │   └── form.html
    │           ├── donations/
    │           │   ├── list.html
    │           │   └── form.html
    │           ├── reports/index.html
    │           ├── users/
    │           │   ├── list.html
    │           │   └── form.html
    │           └── error/access-denied.html
    └── test/
        ├── java/com/smartfood/expiry/
        │   ├── SmartFoodExpiryApplicationTests.java
        │   └── service/
        │       ├── ExpiryDetectionServiceTest.java
        │       ├── FefoServiceTest.java
        │       └── StockValidationAndMovementTest.java
        └── resources/
            └── application-test.properties
```
