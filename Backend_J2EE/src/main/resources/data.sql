-- Sample data for all tables (except review_media)
-- Run this script after creating tables

-- 1. account (5 rows)
INSERT IGNORE INTO account (username, password, email, phone, role) VALUES
('admin', '12345', 'admin@shop.com', '0901234567', 'admin'),
('user1', '12345', 'user1@gmail.com', '0912345678', 'user'),
('user2', '12345', 'user2@gmail.com', '0923456789', 'user'),
('customer1', '12345', 'customer@email.com', '0934567890', 'user');

-- 2. category (4 rows)
INSERT INTO category (name, description)
SELECT 'Áo thun', 'Các loại áo thun nam nữ'
WHERE NOT EXISTS (
	SELECT 1 FROM category WHERE LOWER(TRIM(name)) = LOWER(TRIM('Áo thun'))
);

INSERT INTO category (name, description)
SELECT 'Quần jean', 'Quần jean cao cấp'
WHERE NOT EXISTS (
	SELECT 1 FROM category WHERE LOWER(TRIM(name)) = LOWER(TRIM('Quần jean'))
);

INSERT INTO category (name, description)
SELECT 'Váy', 'Váy nữ đa dạng'
WHERE NOT EXISTS (
	SELECT 1 FROM category WHERE LOWER(TRIM(name)) = LOWER(TRIM('Váy'))
);

INSERT INTO category (name, description)
SELECT 'Phụ kiện', 'Túi, mũ, thắt lưng'
WHERE NOT EXISTS (
	SELECT 1 FROM category WHERE LOWER(TRIM(name)) = LOWER(TRIM('Phụ kiện'))
);

-- 3. product (5 rows)
INSERT INTO product (name, price, description, image, category_id)
SELECT 'Áo thun basic trắng', 199000, 'Áo thun cotton cao cấp', 'ao-thun-trang.jpg', 1
WHERE NOT EXISTS (
	SELECT 1 FROM product WHERE LOWER(TRIM(name)) = LOWER(TRIM('Áo thun basic trắng'))
);

INSERT INTO product (name, price, description, image, category_id)
SELECT 'Quần jean slim fit', 450000, 'Quần jean ôm form', 'quan-jean-slim.jpg', 2
WHERE NOT EXISTS (
	SELECT 1 FROM product WHERE LOWER(TRIM(name)) = LOWER(TRIM('Quần jean slim fit'))
);

INSERT INTO product (name, price, description, image, category_id)
SELECT 'Váy midi hoa nhí', 350000, 'Váy nữ dạo phố', 'vay-midi-hoa.jpg', 3
WHERE NOT EXISTS (
	SELECT 1 FROM product WHERE LOWER(TRIM(name)) = LOWER(TRIM('Váy midi hoa nhí'))
);

INSERT INTO product (name, price, description, image, category_id)
SELECT 'Áo thun form rộng', 249000, 'Áo oversized unisex', 'ao-form-rong.jpg', 1
WHERE NOT EXISTS (
	SELECT 1 FROM product WHERE LOWER(TRIM(name)) = LOWER(TRIM('Áo thun form rộng'))
);

INSERT INTO product (name, price, description, image, category_id)
SELECT 'Túi đeo chéo canvas', 180000, 'Túi canvas thời trang', 'tui-deo-cheo.jpg', 4
WHERE NOT EXISTS (
	SELECT 1 FROM product WHERE LOWER(TRIM(name)) = LOWER(TRIM('Túi đeo chéo canvas'))
);

-- 4. sizes (4 rows)
INSERT IGNORE INTO sizes (size_name) VALUES
('S'), ('M'), ('L'), ('XL');

-- 5. product_sizes (5 rows)
INSERT IGNORE INTO product_sizes (product_id, size_id, quantity) VALUES
(1, 1, 10), (1, 2, 15), (1, 3, 20),
(2, 2, 8), (2, 3, 12);

-- 6. orders (3 rows)
INSERT IGNORE INTO orders (account_id, total_price, status, address, phone) VALUES
(2, 848000, 'completed', '123 Nguyễn Huệ, Q1, TP.HCM', '0912345678'),
(3, 450000, 'shipping', '456 Lê Lợi, Q3, TP.HCM', '0923456789'),
(4, 398000, 'pending', '789 Trần Hưng Đạo, Q5, TP.HCM', '0934567890');

-- 7. order_details (5 rows)
INSERT IGNORE INTO order_details (order_id, product_size_id, quantity, price) VALUES
(1, 1, 2, 199000),
(1, 4, 1, 450000),
(2, 5, 1, 450000),
(3, 1, 1, 199000),
(3, 2, 1, 199000);

-- 8. reviews (4 rows)
INSERT IGNORE INTO reviews (product_id, account_id, rating, comment) VALUES
(1, 2, 5, 'Áo đẹp, chất lượng tốt!'),
(2, 3, 4, 'Quần vừa form, giao hàng nhanh'),
(3, 4, 5, 'Váy xinh lắm, sẽ mua thêm'),
(1, 3, 4, 'Giá hợp lý');

-- 9. product_images (5 rows)
INSERT IGNORE INTO product_images (product_id, image_url, is_main) VALUES
(1, 'ao-thun-trang-1.jpg', 1),
(1, 'ao-thun-trang-2.jpg', 0),
(2, 'quan-jean-slim-1.jpg', 1),
(3, 'vay-midi-hoa-1.jpg', 1),
(4, 'ao-form-rong-1.jpg', 1);
