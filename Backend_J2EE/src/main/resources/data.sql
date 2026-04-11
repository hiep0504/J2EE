-- Demo seed data for the storefront
-- Safe to rerun because all inserts use fixed IDs and upsert existing demo rows.

INSERT INTO category (id, name, description) VALUES
(1, 'Áo thun', 'Áo thun nam nữ'),
(2, 'Quần', 'Quần thời trang nam nữ'),
(3, 'Váy', 'Váy thời trang'),
(4, 'Phụ kiện', 'Túi, nơ, phụ kiện')
ON DUPLICATE KEY UPDATE
name = VALUES(name),
description = VALUES(description);

INSERT INTO sizes (id, size_name) VALUES
(1, 'S'),
(2, 'M'),
(3, 'L'),
(4, 'XL'),
(5, '2XL')
ON DUPLICATE KEY UPDATE
size_name = VALUES(size_name);

INSERT INTO account (id, username, password, email, phone, role, login_type, is_locked, created_at) VALUES
(1001, 'admin', '123456Aa@', 'admin@gmail.com', '0900000001', 'admin', 'local', 0, '2026-04-12 07:00:00'),
(1002, 'user', '123456Aa@', 'user@gmail.com', '0900000002', 'user', 'local', 0, '2026-04-12 07:05:00'),
(1003, 'user2', '123456Aa@', 'user2@gmail.com', '0900000003', 'user', 'local', 0, '2026-04-12 07:10:00'),
(1004, 'user3', '123456Aa@', 'user3@gmail.com', '0900000004', 'user', 'local', 0, '2026-04-12 07:15:00'),
(1005, 'user4', '123456Aa@', 'user4@gmail.com', '0900000005', 'user', 'local', 0, '2026-04-12 07:20:00'),
(1006, 'user5', '123456Aa@', 'user5@gmail.com', '0900000006', 'user', 'local', 0, '2026-04-12 07:25:00')

ON DUPLICATE KEY UPDATE
password = VALUES(password),
email = VALUES(email),
phone = VALUES(phone),
role = VALUES(role),
login_type = VALUES(login_type),
is_locked = VALUES(is_locked);

INSERT INTO product (id, name, price, description, image, category_id, created_at) VALUES
(1, 'Ao thun den ca sau phi hanh gia', 329000.00, 'Ao thun den form tre trung, in hinh ca sau phi hanh gia noi bat.', '/uploads/images/ao-thun-den-ca-sau.jpg', 1, '2026-04-12 08:00:00'),
(2, 'Ao thun EFF YOU SEE KEY', 299000.00, 'Ao thun graphic ca tinh, chat vai mem va thoang.', '/uploads/images/ao-thun-eff-you-see-key.jpg', 1, '2026-04-12 08:05:00'),
(3, 'Ao dai truyen thong', 599000.00, 'Ao dai thanh lich cho su kien va dip le.', '/uploads/images/ao-dai.jpg', 1, '2026-04-12 08:10:00'),
(4, 'Ao polo nam theu ong mat', 389000.00, 'Ao polo nam cao cap, theu ong mat tinh te.', '/uploads/images/ao-polo-ong-mat.jpg', 1, '2026-04-12 08:15:00'),
(5, 'Set do thoi trang nam nai decor', 449000.00, 'Set do nam phong cach hien dai, de phoi trong ngay.', '/uploads/images/set-do-nam-nai-decor.jpg', 1, '2026-04-12 08:20:00'),
(6, 'Quan jean nam rach', 459000.00, 'Quan jean nam rach nhe, phong cach tre trung.', '/uploads/images/quan-jean-nam-rach.jpg', 2, '2026-04-12 08:25:00'),
(7, 'Quan lung dang rong thoai mai', 279000.00, 'Quan lung dang rong, mac nha hoac di choi deu hop.', '/uploads/images/quan-lung-dang-rong.jpg', 2, '2026-04-12 08:30:00'),
(8, 'Quan short jean be trai', 249000.00, 'Quan short jean nang dong danh cho be trai.', '/uploads/images/quan-short-jean-be-trai.jpg', 2, '2026-04-12 08:35:00'),
(9, 'Quan the thao nam nu', 219000.00, 'Quan the thao nang dong, co gian tot.', '/uploads/images/quan-the-thao-nam-nu.jpg', 2, '2026-04-12 08:40:00'),
(10, 'Quan dui cotton xanh la', 179000.00, 'Quan dui cotton nhe thoang, phu hop mua he.', '/uploads/images/quan-dui-cotton-kiza.jpg', 2, '2026-04-12 08:45:00'),
(11, 'Vay cuoi cong chua FELY', 1899000.00, 'Vay cuoi cong chua lap lanh, no bat trong ngay cuoi.', '/uploads/images/vay-cuoi-fely.jpg', 3, '2026-04-12 08:50:00'),
(12, 'Vay dai xoe ngan', 429000.00, 'Vay xoe nu tinh, de mac di choi va su kien nho.', '/uploads/images/vay-dai-xoe-ngan.jpg', 3, '2026-04-12 08:55:00'),
(13, 'Dam maxi nu tinh', 499000.00, 'Dam maxi nhe nhang, phu hop di bien va da ngoai.', '/uploads/images/dam-maxi.jpg', 3, '2026-04-12 09:00:00'),
(14, 'Day buoc toc no hong', 59000.00, 'Phu kien buoc toc no hong xinh xan.', '/uploads/images/day-buoc-toc-no-hong.jpg', 4, '2026-04-12 09:05:00'),
(15, 'Tui Helicopi', 219000.00, 'Tui deo phong cach Helicopi, gon nhe va tien dung.', '/uploads/images/tui-helicopi.jpg', 4, '2026-04-12 09:10:00')
ON DUPLICATE KEY UPDATE
name = VALUES(name),
price = VALUES(price),
description = VALUES(description),
image = VALUES(image),
category_id = VALUES(category_id),
created_at = VALUES(created_at);

INSERT INTO product_sizes (product_id, size_id, quantity)
SELECT p.id, s.id, 20
FROM product p
JOIN sizes s ON s.id BETWEEN 1 AND 5
WHERE p.id BETWEEN 1 AND 15
ON DUPLICATE KEY UPDATE
quantity = VALUES(quantity);

INSERT INTO reviews (id, product_id, account_id, rating, comment, created_at)
SELECT 2001, 1, a.id, 5, 'San pham dep, chat lieu on.', '2026-04-12 10:00:00'
FROM account a
WHERE a.username = 'user'
ON DUPLICATE KEY UPDATE
account_id = VALUES(account_id),
rating = VALUES(rating),
comment = VALUES(comment),
created_at = VALUES(created_at);

INSERT INTO reviews (id, product_id, account_id, rating, comment, created_at)
SELECT 2002, 2, a.id, 4, 'Mac dep, dang ao de phoi do.', '2026-04-12 10:05:00'
FROM account a
WHERE a.username = 'user'
ON DUPLICATE KEY UPDATE
account_id = VALUES(account_id),
rating = VALUES(rating),
comment = VALUES(comment),
created_at = VALUES(created_at);

INSERT INTO reviews (id, product_id, account_id, rating, comment, created_at)
SELECT 2003, 6, a.id, 5, 'Quan jean form dep, gia hop ly.', '2026-04-12 10:10:00'
FROM account a
WHERE a.username = 'admin'
ON DUPLICATE KEY UPDATE
account_id = VALUES(account_id),
rating = VALUES(rating),
comment = VALUES(comment),
created_at = VALUES(created_at);

INSERT INTO reviews (id, product_id, account_id, rating, comment, created_at)
SELECT 2004, 11, a.id, 5, 'Vay dep va no bat cho su kien.', '2026-04-12 10:15:00'
FROM account a
WHERE a.username = 'user'
ON DUPLICATE KEY UPDATE
account_id = VALUES(account_id),
rating = VALUES(rating),
comment = VALUES(comment),
created_at = VALUES(created_at);

INSERT INTO orders (id, account_id, order_date, total_price, status, address, phone) VALUES
(4001, (SELECT id FROM account WHERE username = 'user' LIMIT 1),  '2026-02-13 09:00:00', 329000.00, 'completed', '12 Nguyen Trai, Q1, TP.HCM', '0900000002'),
(4002, (SELECT id FROM account WHERE username = 'user' LIMIT 1),  '2026-02-13 10:00:00', 459000.00, 'completed', '12 Nguyen Trai, Q1, TP.HCM', '0900000002'),
(4003, (SELECT id FROM account WHERE username = 'user2' LIMIT 1), '2026-03-13 11:00:00', 299000.00, 'completed', '45 Le Loi, Q3, TP.HCM', '0900000003'),
(4004, (SELECT id FROM account WHERE username = 'user2' LIMIT 1), '2026-04-13 12:00:00', 429000.00, 'completed', '45 Le Loi, Q3, TP.HCM', '0900000003'),
(4005, (SELECT id FROM account WHERE username = 'user3' LIMIT 1), '2026-02-13 13:00:00', 389000.00, 'completed', '80 Hai Ba Trung, Q3, TP.HCM', '0900000004'),
(4006, (SELECT id FROM account WHERE username = 'user3' LIMIT 1), '2026-03-13 14:00:00', 219000.00, 'completed', '80 Hai Ba Trung, Q3, TP.HCM', '0900000004'),
(4007, (SELECT id FROM account WHERE username = 'user4' LIMIT 1), '2026-03-13 15:00:00', 599000.00, 'completed', '22 Vo Van Tan, Q3, TP.HCM', '0900000005'),
(4008, (SELECT id FROM account WHERE username = 'user4' LIMIT 1), '2026-02-13 16:00:00', 179000.00, 'completed', '22 Vo Van Tan, Q3, TP.HCM', '0900000005'),
(4009, (SELECT id FROM account WHERE username = 'user5' LIMIT 1), '2026-04-13 17:00:00', 499000.00, 'completed', '99 Tran Hung Dao, Q5, TP.HCM', '0900000006'),
(4010, (SELECT id FROM account WHERE username = 'user5' LIMIT 1), '2026-02-13 18:00:00', 219000.00, 'completed', '99 Tran Hung Dao, Q5, TP.HCM', '0900000006')
ON DUPLICATE KEY UPDATE
account_id = VALUES(account_id),
order_date = VALUES(order_date),
total_price = VALUES(total_price),
status = VALUES(status),
address = VALUES(address),
phone = VALUES(phone);

INSERT INTO order_details (id, order_id, product_size_id, quantity, price) VALUES
(5001, 4001, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 1  AND ps.size_id = 2 LIMIT 1), 1, 329000.00),
(5002, 4002, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 6  AND ps.size_id = 3 LIMIT 1), 1, 459000.00),
(5003, 4003, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 2  AND ps.size_id = 2 LIMIT 1), 1, 299000.00),
(5004, 4004, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 12 AND ps.size_id = 2 LIMIT 1), 1, 429000.00),
(5005, 4005, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 4  AND ps.size_id = 3 LIMIT 1), 1, 389000.00),
(5006, 4006, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 9  AND ps.size_id = 2 LIMIT 1), 1, 219000.00),
(5007, 4007, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 3  AND ps.size_id = 2 LIMIT 1), 1, 599000.00),
(5008, 4008, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 10 AND ps.size_id = 3 LIMIT 1), 1, 179000.00),
(5009, 4009, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 13 AND ps.size_id = 2 LIMIT 1), 1, 499000.00),
(5010, 4010, (SELECT ps.id FROM product_sizes ps WHERE ps.product_id = 15 AND ps.size_id = 5 LIMIT 1), 1, 219000.00)
ON DUPLICATE KEY UPDATE
order_id = VALUES(order_id),
product_size_id = VALUES(product_size_id),
quantity = VALUES(quantity),
price = VALUES(price);

INSERT INTO review_media (id, review_id, media_url, media_type, created_at)
SELECT 3001, 2001, '/uploads/images/ao-thun-den-ca-sau.jpg', 'image', '2026-04-12 10:20:00'
FROM reviews r
WHERE r.id = 2001
ON DUPLICATE KEY UPDATE
review_id = VALUES(review_id),
media_url = VALUES(media_url),
media_type = VALUES(media_type),
created_at = VALUES(created_at);

INSERT INTO review_media (id, review_id, media_url, media_type, created_at)
SELECT 3002, 2002, '/uploads/images/ao-thun-eff-you-see-key.jpg', 'image', '2026-04-12 10:21:00'
FROM reviews r
WHERE r.id = 2002
ON DUPLICATE KEY UPDATE
review_id = VALUES(review_id),
media_url = VALUES(media_url),
media_type = VALUES(media_type),
created_at = VALUES(created_at);

INSERT INTO review_media (id, review_id, media_url, media_type, created_at)
SELECT 3003, 2003, '/uploads/images/quan-jean-nam-rach.jpg', 'image', '2026-04-12 10:22:00'
FROM reviews r
WHERE r.id = 2003
ON DUPLICATE KEY UPDATE
review_id = VALUES(review_id),
media_url = VALUES(media_url),
media_type = VALUES(media_type),
created_at = VALUES(created_at);

INSERT INTO review_media (id, review_id, media_url, media_type, created_at)
SELECT 3004, 2004, '/uploads/images/vay-cuoi-fely.jpg', 'image', '2026-04-12 10:23:00'
FROM reviews r
WHERE r.id = 2004
ON DUPLICATE KEY UPDATE
review_id = VALUES(review_id),
media_url = VALUES(media_url),
media_type = VALUES(media_type),
created_at = VALUES(created_at);

-- Fix legacy review image URLs that point to old UUID files no longer present.
UPDATE review_media rm
JOIN reviews r ON r.id = rm.review_id
JOIN product p ON p.id = r.product_id
SET rm.media_url = p.image
WHERE rm.media_type = 'image'
	AND rm.media_url LIKE '/uploads/images/%-%-%-%-%';

INSERT INTO product_images (id, product_id, image_url, is_main, created_at) VALUES
(1, 1, '/uploads/images/ao-thun-den-ca-sau.jpg', 1, '2026-04-12 09:20:00'),
(2, 2, '/uploads/images/ao-thun-eff-you-see-key.jpg', 1, '2026-04-12 09:21:00'),
(3, 3, '/uploads/images/ao-dai.jpg', 1, '2026-04-12 09:22:00'),
(4, 4, '/uploads/images/ao-polo-ong-mat.jpg', 1, '2026-04-12 09:23:00'),
(5, 5, '/uploads/images/set-do-nam-nai-decor.jpg', 1, '2026-04-12 09:24:00'),
(6, 6, '/uploads/images/quan-jean-nam-rach.jpg', 1, '2026-04-12 09:25:00'),
(7, 7, '/uploads/images/quan-lung-dang-rong.jpg', 1, '2026-04-12 09:26:00'),
(8, 8, '/uploads/images/quan-short-jean-be-trai.jpg', 1, '2026-04-12 09:27:00'),
(9, 9, '/uploads/images/quan-the-thao-nam-nu.jpg', 1, '2026-04-12 09:28:00'),
(10, 10, '/uploads/images/quan-dui-cotton-kiza.jpg', 1, '2026-04-12 09:29:00'),
(11, 11, '/uploads/images/vay-cuoi-fely.jpg', 1, '2026-04-12 09:30:00'),
(12, 12, '/uploads/images/vay-dai-xoe-ngan.jpg', 1, '2026-04-12 09:31:00'),
(13, 13, '/uploads/images/dam-maxi.jpg', 1, '2026-04-12 09:32:00'),
(14, 14, '/uploads/images/day-buoc-toc-no-hong.jpg', 1, '2026-04-12 09:33:00'),
(15, 15, '/uploads/images/tui-helicopi.jpg', 1, '2026-04-12 09:34:00')
ON DUPLICATE KEY UPDATE
product_id = VALUES(product_id),
image_url = VALUES(image_url),
is_main = VALUES(is_main),
created_at = VALUES(created_at);
