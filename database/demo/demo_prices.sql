-- Giá bán DEMO cho các sản phẩm demo đang kinh doanh, để thử tạo đơn hàng (đơn hàng chỉ thêm được sản phẩm đã có giá).
-- Điều kiện: đã nạp demo_catalog.sql và có ba bảng giá BG-DL1 (Đại lý cấp 1), BG-DL2 (Đại lý cấp 2), BG-LE (Khách lẻ).
-- Giá bán = giá vốn nhân hệ số theo nhóm khách (cấp 1 khoảng +10%, cấp 2 khoảng +15%, khách lẻ khoảng +22%), lệch nhẹ
-- theo từng sản phẩm cho đa dạng, làm tròn đến 100 đồng; giá sàn = giá vốn +3%, không vượt giá bán.
-- Chạy lại nhiều lần an toàn (chỉ thêm dòng giá chưa có). Ghi cả lịch sử giá (loại "Thêm vào bảng giá", người sửa: admin).
-- Gỡ cùng bộ demo bằng demo_catalog_remove.sql.
USE oms;

INSERT INTO price_list_items (price_list_id, product_id, sale_price, floor_price)
SELECT pl.id, p.id,
       s.sale_price,
       LEAST(s.sale_price, GREATEST(p.cost_price, ROUND(p.cost_price * 1.03 / 100) * 100))
FROM price_lists pl
JOIN products p ON p.status = 'ACTIVE' AND p.cost_price IS NOT NULL
JOIN (SELECT 'BG-DL1' AS code, 1.10 AS base UNION ALL SELECT 'BG-DL2', 1.15 UNION ALL SELECT 'BG-LE', 1.22) m ON m.code = pl.code
JOIN (SELECT p2.id AS product_id, pl2.id AS list_id,
             ROUND(p2.cost_price * (m2.base + MOD(p2.id, 4) * 0.01) / 100) * 100 AS sale_price
      FROM price_lists pl2
      JOIN products p2 ON p2.status = 'ACTIVE' AND p2.cost_price IS NOT NULL
      JOIN (SELECT 'BG-DL1' AS code, 1.10 AS base UNION ALL SELECT 'BG-DL2', 1.15 UNION ALL SELECT 'BG-LE', 1.22) m2 ON m2.code = pl2.code) s
  ON s.product_id = p.id AND s.list_id = pl.id
WHERE NOT EXISTS (SELECT 1 FROM price_list_items i WHERE i.price_list_id = pl.id AND i.product_id = p.id);

-- Lịch sử giá cho các dòng vừa thêm (chỉ khi sản phẩm chưa có lịch sử ở bảng giá đó)
INSERT INTO price_history (product_id, price_list_id, change_type, old_sale_price, old_floor_price,
                           new_sale_price, new_floor_price, effective_from, changed_by)
SELECT i.product_id, i.price_list_id, 'ADD', NULL, NULL, i.sale_price, i.floor_price, pl.valid_from,
       (SELECT id FROM users WHERE username = 'admin' LIMIT 1)
FROM price_list_items i
JOIN price_lists pl ON pl.id = i.price_list_id
WHERE EXISTS (SELECT 1 FROM users WHERE username = 'admin')
  AND NOT EXISTS (SELECT 1 FROM price_history h WHERE h.product_id = i.product_id AND h.price_list_id = i.price_list_id);
