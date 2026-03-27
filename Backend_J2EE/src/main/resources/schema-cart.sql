-- Add Cart table
CREATE TABLE IF NOT EXISTS cart (
    id INT PRIMARY KEY AUTO_INCREMENT,
    account_id INT NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE
);

-- Add CartItem table
CREATE TABLE IF NOT EXISTS cart_item (
    id INT PRIMARY KEY AUTO_INCREMENT,
    cart_id INT NOT NULL,
    product_id INT NOT NULL,
    size_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price DECIMAL(10, 2),
    FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    FOREIGN KEY (size_id) REFERENCES sizes(id) ON DELETE CASCADE,
    UNIQUE KEY uk_cart_product_size (cart_id, product_id, size_id)
);

-- Add unique constraint on product_sizes if not exists
ALTER TABLE product_sizes ADD CONSTRAINT uk_product_size UNIQUE (product_id, size_id);

-- Clean up duplicate product_sizes (keep first, delete rest)
DELETE FROM product_sizes 
WHERE id NOT IN (
    SELECT MIN(id) 
    FROM (
        SELECT * FROM product_sizes
    ) AS temp
    GROUP BY product_id, size_id
);
