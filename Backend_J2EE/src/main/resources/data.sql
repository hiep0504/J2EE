INSERT IGNORE INTO account (username, password, email, phone, role) VALUES
('admin', '12345', 'admin@shop.com', '0901234567', 'admin'),
('user1', '12345', 'user1@gmail.com', '0912345678', 'user'),
('user2', '12345', 'user2@gmail.com', '0923456789', 'user'),
('customer1', '12345', 'customer@email.com', '0934567890', 'user');

-- 2. category (4 rows)
INSERT IGNORE INTO category (name, description) VALUES
('Áo thun', 'Các loại áo thun nam nữ'),
('Quần jean', 'Quần jean cao cấp'),
('Váy', 'Váy nữ đa dạng'),
('Phụ kiện', 'Túi, mũ, thắt lưng');

-- 3. product (5 rows)
INSERT IGNORE INTO product (name, price, description, image, category_id) VALUES
('Áo thun basic trắng', 199000, 'Áo thun cotton cao cấp', 'ao-thun-trang.jpg', 1),
('Quần jean slim fit', 450000, 'Quần jean ôm form', 'quan-jean-slim.jpg', 2),
('Váy midi hoa nhí', 350000, 'Váy nữ dạo phố', 'vay-midi-hoa.jpg', 3),
('Áo thun form rộng', 249000, 'Áo oversized unisex', 'ao-form-rong.jpg', 1),
('Túi đeo chéo canvas', 180000, 'Túi canvas thời trang', 'tui-deo-cheo.jpg', 4);

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
