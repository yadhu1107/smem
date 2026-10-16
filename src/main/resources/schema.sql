-- ===================================================================
-- Database: food_expiry_db
-- Schema Definition (Idempotent: CREATE TABLE IF NOT EXISTS)
-- ===================================================================

CREATE DATABASE IF NOT EXISTS food_expiry_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE food_expiry_db;

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    default_warning_days INT NOT NULL DEFAULT 7,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 3. Products table
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    brand VARCHAR(100) NOT NULL,
    barcode VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500),
    unit VARCHAR(30) NOT NULL DEFAULT 'pcs',
    min_stock_quantity INT NOT NULL DEFAULT 10,
    purchase_price DECIMAL(10,2) DEFAULT 0.00,
    selling_price DECIMAL(10,2) DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON UPDATE CASCADE
) ENGINE=InnoDB;

-- 4. Product Batches table
CREATE TABLE IF NOT EXISTS product_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    batch_number VARCHAR(60) NOT NULL,
    quantity INT NOT NULL,
    initial_quantity INT NOT NULL,
    purchase_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    cost_per_unit DECIMAL(10,2) DEFAULT 0.00,
    selling_price_per_unit DECIMAL(10,2) DEFAULT 0.00,
    supplier VARCHAR(100),
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_batches_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_batches_expiry_date (expiry_date),
    INDEX idx_batches_qty (quantity)
) ENGINE=InnoDB;

-- 5. Stock Movements table (audit trail)
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    movement_type VARCHAR(30) NOT NULL,
    quantity INT NOT NULL,
    previous_quantity INT NOT NULL,
    new_quantity INT NOT NULL,
    reason VARCHAR(200),
    performed_by_user_id BIGINT,
    movement_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    CONSTRAINT fk_movement_batch FOREIGN KEY (batch_id) REFERENCES product_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_movement_user FOREIGN KEY (performed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_movement_date (movement_date)
) ENGINE=InnoDB;

-- 6. Expiry Notifications table
CREATE TABLE IF NOT EXISTS expiry_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    alert_type VARCHAR(20) NOT NULL,
    alert_date DATE NOT NULL,
    days_remaining BIGINT NOT NULL,
    is_reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_by_user_id BIGINT,
    reviewed_at DATETIME,
    notes VARCHAR(500),
    CONSTRAINT fk_notif_batch FOREIGN KEY (batch_id) REFERENCES product_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_user FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_notif_reviewed (is_reviewed)
) ENGINE=InnoDB;

-- 7. Waste Records table
CREATE TABLE IF NOT EXISTS waste_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    waste_reason VARCHAR(30) NOT NULL,
    waste_date DATE NOT NULL,
    recorded_by_user_id BIGINT,
    estimated_cost_loss DECIMAL(10,2) DEFAULT 0.00,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_waste_batch FOREIGN KEY (batch_id) REFERENCES product_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_waste_user FOREIGN KEY (recorded_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_waste_date (waste_date)
) ENGINE=InnoDB;

-- 8. Donation Records table
CREATE TABLE IF NOT EXISTS donation_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    recipient_organization VARCHAR(150) NOT NULL,
    quantity INT NOT NULL,
    donation_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    recorded_by_user_id BIGINT,
    notes VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_donation_batch FOREIGN KEY (batch_id) REFERENCES product_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_donation_user FOREIGN KEY (recorded_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_donation_date (donation_date)
) ENGINE=InnoDB;
