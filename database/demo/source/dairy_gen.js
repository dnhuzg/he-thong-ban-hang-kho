// Sinh database/demo/dairy_demo.sql từ danh sách bên dưới. Cần Node.js: node database/demo/source/dairy_gen.js database/demo/dairy_demo.sql
const fs = require("fs");
const q = (s) => (s == null ? "NULL" : "'" + String(s).replace(/'/g, "''") + "'");

const SUPPLIERS = [
  ["VINAMILK", "Công ty CP Sữa Việt Nam (Vinamilk)", "Phòng kinh doanh", "Công nợ 30 ngày"],
  ["NCC-TH", "Công ty CP Sữa TH", "Phòng kinh doanh", "Công nợ 30 ngày"],
  ["NCC-FCV", "Công ty TNHH FrieslandCampina Việt Nam", "Phòng kinh doanh", "Công nợ 15 ngày"],
  ["NCC-NUTI", "Công ty CP Thực phẩm Dinh dưỡng Nutifood", "Phòng kinh doanh", "Thanh toán khi nhận hàng"],
];

// [nhóm, ĐVT cơ sở, quy đổi, hạn dùng (ngày), cảnh báo trước (ngày), [[sku, tên, giá vốn, NCC]]]
const BLOCKS = [
  ["Sữa tươi tiệt trùng", "HOP", { LOC: 4, THUNG: 48 }, 180, 30, [
    ["VNM-STT-180-CD", "Sữa tươi tiệt trùng Vinamilk 100% có đường 180ml", 6500, "VINAMILK"],
    ["VNM-STT-180-KD", "Sữa tươi tiệt trùng Vinamilk 100% không đường 180ml", 6500, "VINAMILK"],
    ["VNM-STT-110-CD", "Sữa tươi tiệt trùng Vinamilk 100% có đường 110ml", 4600, "VINAMILK"],
    ["TH-STT-180-CD", "Sữa tươi tiệt trùng TH true MILK có đường 180ml", 7400, "NCC-TH"],
    ["TH-STT-180-IT", "Sữa tươi tiệt trùng TH true MILK ít đường 180ml", 7400, "NCC-TH"],
    ["TH-STT-110-CD", "Sữa tươi tiệt trùng TH true MILK có đường 110ml", 5000, "NCC-TH"],
    ["DL-STT-180-CD", "Sữa tiệt trùng Dutch Lady có đường 180ml", 6300, "NCC-FCV"],
    ["DL-STT-110-CD", "Sữa tiệt trùng Dutch Lady có đường 110ml", 4300, "NCC-FCV"],
    ["NTF-STT-180-CD", "Sữa tiệt trùng Nutimilk có đường 180ml", 5800, "NCC-NUTI"],
  ]],
  ["Sữa tươi tiệt trùng", "HOP", { THUNG: 12 }, 180, 30, [
    ["VNM-STT-1L-CD", "Sữa tươi tiệt trùng Vinamilk 100% có đường hộp 1 lít", 32000, "VINAMILK"],
    ["TH-STT-1L-CD", "Sữa tươi tiệt trùng TH true MILK có đường hộp 1 lít", 36000, "NCC-TH"],
  ]],
  ["Sữa tươi thanh trùng", "CHAI", { THUNG: 20 }, 10, 3, [
    ["TH-STTR-450", "Sữa tươi thanh trùng TH true MILK nguyên chất chai 450ml", 17000, "NCC-TH"],
    ["VNM-STTR-450", "Sữa tươi thanh trùng Vinamilk 100% chai 450ml", 16000, "VINAMILK"],
  ]],
  ["Sữa chua ăn", "HU", { LOC: 4, THUNG: 48 }, 45, 10, [
    ["VNM-SCA-100-CD", "Sữa chua ăn Vinamilk có đường hũ 100g", 5300, "VINAMILK"],
    ["VNM-SCA-100-KD", "Sữa chua ăn Vinamilk không đường hũ 100g", 5300, "VINAMILK"],
    ["VNM-SCA-100-ND", "Sữa chua ăn Vinamilk nha đam hũ 100g", 5800, "VINAMILK"],
    ["TH-SCA-100-CD", "Sữa chua ăn TH true YOGURT có đường hũ 100g", 6500, "NCC-TH"],
  ]],
  ["Sữa chua uống", "HOP", { LOC: 4, THUNG: 48 }, 60, 10, [
    ["DL-YOMOST-170", "Sữa chua uống Yomost hương dâu 170ml", 6500, "NCC-FCV"],
    ["TH-SCU-180", "Sữa chua uống TH true YOGURT men sống 180ml", 8000, "NCC-TH"],
  ]],
  ["Sữa chua uống", "CHAI", { LOC: 5, THUNG: 50 }, 45, 10, [
    ["VNM-PROBI-65", "Sữa chua uống Probi Vinamilk có đường chai 65ml", 4000, "VINAMILK"],
  ]],
  ["Sữa bột trẻ em", "LON", { THUNG: 12 }, 730, 60, [
    ["VNM-DIELAC-900", "Sữa bột Dielac Grow Plus 2+ lon 900g", 245000, "VINAMILK"],
    ["VNM-OPTIMUM-850", "Sữa bột Optimum Gold 4 lon 850g", 320000, "VINAMILK"],
    ["DL-FRISO-850", "Sữa bột Friso Gold 4 lon 850g", 470000, "NCC-FCV"],
    ["NTF-NUTIIQ-900", "Sữa bột Nuti IQ Gold 4 lon 900g", 230000, "NCC-NUTI"],
  ]],
  ["Sữa bột trẻ em", "LON", { THUNG: 6 }, 730, 60, [
    ["NTF-GROWPLUS-1.5", "Sữa bột GrowPLUS+ đỏ lon 1,5kg", 420000, "NCC-NUTI"],
  ]],
  ["Sữa bột người lớn", "LON", { THUNG: 12 }, 730, 60, [
    ["VNM-SURE-900", "Sữa bột Sure Prevent Gold lon 900g", 380000, "VINAMILK"],
    ["VNM-CANXI-900", "Sữa bột Canxi Pro lon 900g", 245000, "VINAMILK"],
    ["NTF-VARNA-850", "Sữa bột Varna Complete lon 850g", 495000, "NCC-NUTI"],
  ]],
  ["Sữa đặc", "LON", { THUNG: 48 }, 365, 30, [
    ["VNM-ONGTHO-380", "Sữa đặc có đường Ông Thọ trắng lon 380g", 22500, "VINAMILK"],
    ["VNM-ONGTHO-DO-380", "Sữa đặc có đường Ông Thọ đỏ lon 380g", 21000, "VINAMILK"],
    ["VNM-NGOISAO-380", "Sữa đặc có đường Ngôi Sao Phương Nam xanh lá lon 380g", 19000, "VINAMILK"],
  ]],
  ["Sữa hạt", "HOP", { LOC: 4, THUNG: 48 }, 270, 30, [
    ["VNM-HAT-OCCHO-180", "Sữa hạt óc chó Vinamilk 180ml", 8500, "VINAMILK"],
    ["TH-HAT-MACCA-180", "Sữa hạt macca TH true NUT 180ml", 9000, "NCC-TH"],
  ]],
  ["Phô mai, bơ, kem", "HOP", { THUNG: 12 }, 365, 30, [
    ["VNM-KEM-VANI-450", "Kem Vinamilk vani hộp 450ml", 45000, "VINAMILK"],
    ["TH-BO-200", "Bơ lạt TH true BUTTER 200g", 55000, "NCC-TH"],
  ]],
];

const products = [];
for (const [cat, base, conv, shelf, warn, items] of BLOCKS)
  for (const [sku, name, cost, sup] of items) products.push({ sku, name, cost, sup, cat, base, conv, shelf, warn });
const skus = new Set(products.map((p) => p.sku));
if (skus.size !== products.length) throw new Error("SKU trùng");

const UNIT_NAME = { HOP: "hộp", LOC: "lốc", THUNG: "thùng", LON: "lon", CHAI: "chai", HU: "hũ" };
const packing = (p) => `${p.conv.THUNG} ${UNIT_NAME[p.base]}/thùng`;

const out = [];
out.push(`-- Dữ liệu DEMO kho sữa: nhà cung cấp, ${products.length} sản phẩm sữa kèm quy đổi, giá trong 3 bảng giá và tồn kho đầu kỳ
-- (phiếu nhập đã xác nhận, có lô và hạn dùng) ở mọi kho đang hoạt động.
-- Chạy sau schema.sql + seed.sql và sau khi khởi động web lần đầu (cần tài khoản admin để đứng tên phiếu nhập).
-- Chạy lại nhiều lần an toàn: chỉ thêm cái chưa có. Sinh từ database/demo/source/dairy_gen.js: sửa danh sách ở đó rồi sinh lại, không sửa tay file này.
-- Chạy:  Get-Content database\\demo\\dairy_demo.sql -Raw -Encoding UTF8 | mysql -uoms_app --default-character-set=utf8mb4
USE oms;

INSERT IGNORE INTO units (code, name) VALUES
  ('HOP', 'Hộp'), ('LOC', 'Lốc'), ('THUNG', 'Thùng'), ('LON', 'Lon'), ('CHAI', 'Chai'), ('HU', 'Hũ');
`);

out.push("-- Nhà cung cấp");
out.push("INSERT IGNORE INTO suppliers (code, name, contact_name, payment_terms) VALUES");
out.push(SUPPLIERS.map(([c, n, ct, t]) => `  (${q(c)}, ${q(n)}, ${q(ct)}, ${q(t)})`).join(",\n") + ";\n");

out.push("-- Sản phẩm: theo dõi lô và hạn dùng, cảnh báo sắp hết hạn theo loại sữa");
out.push("INSERT IGNORE INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price, track_lots, expiry_warning_days)");
out.push(products.map((p) =>
  `SELECT ${q(p.sku)}, ${q(p.name)}, (SELECT id FROM product_categories WHERE name = ${q(p.cat)} LIMIT 1), (SELECT id FROM units WHERE code = ${q(p.base)}), ${q(packing(p))}, ${p.cost}.00, TRUE, ${p.warn}`
).join(" UNION ALL\n") + ";\n");

out.push("-- Quy đổi đơn vị");
out.push("INSERT IGNORE INTO product_units (product_id, unit_id, factor)");
const convRows = [];
for (const p of products) for (const [u, f] of Object.entries(p.conv))
  convRows.push(`SELECT p.id, u.id, ${f} FROM products p JOIN units u ON u.code = ${q(u)} WHERE p.sku = ${q(p.sku)}`);
out.push(convRows.join(" UNION ALL\n") + ";\n");

out.push(`-- Giá bán = giá vốn × hệ số theo nhóm đại lý, giá sàn thấp hơn một chút (làm tròn trăm đồng)
INSERT IGNORE INTO price_list_items (price_list_id, product_id, sale_price, floor_price)
SELECT pl.id, p.id,
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.10 WHEN 'BG-DL2' THEN 1.15 ELSE 1.25 END, -2),
       ROUND(p.cost_price * CASE pl.code WHEN 'BG-DL1' THEN 1.05 WHEN 'BG-DL2' THEN 1.08 ELSE 1.15 END, -2)
FROM price_lists pl JOIN products p ON p.sku IN (${products.map((p) => q(p.sku)).join(", ")})
WHERE pl.code IN ('BG-DL1', 'BG-DL2', 'BG-LE') AND (pl.valid_to IS NULL OR pl.valid_to >= CURDATE());
`);

// Tồn đầu kỳ: mỗi kho × mỗi NCC một phiếu nhập; kho đầu nhiều hàng nhất
const RECEIVED = "2026-10-20", MFG = "2026-10-15";
const addDays = (d, n) => { const t = new Date(d + "T00:00:00Z"); t.setUTCDate(t.getUTCDate() + n); return t.toISOString().slice(0, 10); };
out.push(`-- Tồn đầu kỳ: phiếu nhập đã xác nhận, ghi đúng như GoodsReceiptService (phiếu, dòng, lô, sổ biến động, tồn)
SET @actor = (SELECT MIN(u.id) FROM users u JOIN user_roles r ON r.user_id = u.id AND r.role_code = 'ADMIN');
`);
const WAREHOUSE_CASES = [["1", 20], ["2", 10], ["3", 5]]; // thứ tự kho theo id → số thùng mỗi sản phẩm
for (const [rank, cases] of WAREHOUSE_CASES) {
  for (const [sup] of SUPPLIERS) {
    const doc = `DEMO-K${rank}-${sup}`;
    const lines = products.filter((p) => p.sup === sup);
    out.push(`-- Kho thứ ${rank} · ${sup}`);
    out.push(`SET @wh = (SELECT id FROM warehouses WHERE status = 'ACTIVE' ORDER BY id LIMIT ${Number(rank) - 1}, 1);
SET @sup = (SELECT id FROM suppliers WHERE code = ${q(sup)});
INSERT INTO goods_receipts (supplier_id, document_no, received_date, warehouse_id, note, status, created_by, confirmed_by, confirmed_at)
SELECT @sup, ${q(doc)}, '${RECEIVED}', @wh, 'Tồn đầu kỳ (dữ liệu demo)', 'CONFIRMED', @actor, @actor, '${RECEIVED} 08:00:00'
WHERE @wh IS NOT NULL AND @actor IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipts WHERE supplier_id = @sup AND document_no = ${q(doc)});
SET @r = (SELECT id FROM goods_receipts WHERE supplier_id = @sup AND document_no = ${q(doc)});
UPDATE goods_receipts SET code = CONCAT('PN', LPAD(id, 6, '0')) WHERE id = @r AND code IS NULL;
SET @new = @r IS NOT NULL AND NOT EXISTS (SELECT 1 FROM goods_receipt_lines WHERE receipt_id = @r);
CREATE TEMPORARY TABLE IF NOT EXISTS demo_lines (line_no INT, sku VARCHAR(50), qty INT, lot_no VARCHAR(50), expiry DATE);
TRUNCATE TABLE demo_lines;
INSERT INTO demo_lines VALUES
${lines.map((p, i) => `  (${i + 1}, ${q(p.sku)}, ${cases}, ${q("L" + MFG.replace(/-/g, "").slice(2) + "-" + p.sku)}, '${addDays(MFG, p.shelf)}')`).join(",\n")};
INSERT INTO goods_receipt_lines (receipt_id, line_no, product_id, unit_id, quantity, factor, base_quantity, manufactured_date, lot_no, expiry_date)
SELECT @r, d.line_no, p.id, u.id, d.qty, pu.factor, d.qty * pu.factor, '${MFG}', d.lot_no, d.expiry
FROM demo_lines d JOIN products p ON p.sku = d.sku JOIN units u ON u.code = 'THUNG' JOIN product_units pu ON pu.product_id = p.id AND pu.unit_id = u.id
WHERE @new;
INSERT INTO inventory_lots (warehouse_id, product_id, remaining_quantity, unit_cost, received_at, source_type, source_id, lot_no, manufactured_date, expiry_date)
SELECT @wh, l.product_id, l.base_quantity, COALESCE(p.cost_price, 0), '${RECEIVED} 00:00:00', 'GOODS_RECEIPT', @r, l.lot_no, l.manufactured_date, l.expiry_date
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO inventory_movements (warehouse_id, product_id, unit_id, quantity, movement_type, reference_type, reference_id, created_by, created_at)
SELECT @wh, l.product_id, p.base_unit_id, l.base_quantity, 'GOODS_RECEIPT', 'GOODS_RECEIPT', @r, @actor, '${RECEIVED} 08:00:00'
FROM goods_receipt_lines l JOIN products p ON p.id = l.product_id WHERE @new AND l.receipt_id = @r;
INSERT INTO warehouse_inventory (warehouse_id, product_id, on_hand, reserved)
SELECT @wh, l.product_id, l.base_quantity, 0 FROM goods_receipt_lines l WHERE @new AND l.receipt_id = @r
ON DUPLICATE KEY UPDATE on_hand = on_hand + VALUES(on_hand);
`);
  }
}
out.push("DROP TEMPORARY TABLE IF EXISTS demo_lines;");

// Mật khẩu demo công khai trong README, chỉ dùng cho máy thử; INSERT IGNORE nên không đổi mật khẩu tài khoản đã có
const DEMO_HASH = "$2a$10$bGBhVjc9OUCnd901MXybbOb/wP0xA8fGIkD0.uRUbmDUVB2eJDE56"; // Demo@2026
const DEMO_USERS = [
  ["demo_quanly", "Demo Quản lý kinh doanh", "SALES_MANAGER"],
  ["demo_nvkd", "Demo Nhân viên kinh doanh", "SALES_REP"],
  ["demo_ketoan", "Demo Kế toán công nợ", "ACCOUNTANT"],
  ["demo_kho", "Demo Thủ kho", "WAREHOUSE"],
  ["demo_qlkho", "Demo Quản lý kho", "WH_MANAGER"],
  ["demo_daily", "Demo Đại lý Minh Anh", "CUSTOMER"],
];
out.push(`
-- Tài khoản demo cho 6 vai trò (mật khẩu Demo@2026, không bắt đổi); admin do ứng dụng tự tạo lần chạy đầu
INSERT IGNORE INTO users (username, email, full_name, password_hash, status, must_change_password) VALUES
${DEMO_USERS.map(([u, n]) => `  (${q(u)}, ${q(u + "@oms.local")}, ${q(n)}, ${q(DEMO_HASH)}, 'ACTIVE', 0)`).join(",\n")};
INSERT IGNORE INTO user_roles (user_id, role_code)
${DEMO_USERS.map(([u, , r]) => `SELECT id, ${q(r)} FROM users WHERE username = ${q(u)}`).join(" UNION ALL\n")};
-- Thủ kho và quản lý kho phụ trách kho đầu tiên; nhân viên kinh doanh phụ trách địa bàn và hai đại lý đầu
INSERT IGNORE INTO user_warehouses (user_id, warehouse_id)
SELECT u.id, (SELECT MIN(id) FROM warehouses WHERE status = 'ACTIVE') FROM users u WHERE u.username IN ('demo_kho', 'demo_qlkho');
INSERT IGNORE INTO user_areas (user_id, area_id)
SELECT u.id, c.area_id FROM users u JOIN customers c ON c.code IN ('DL001', 'DL002') WHERE u.username = 'demo_nvkd' AND c.area_id IS NOT NULL;
UPDATE customers SET sales_rep_id = (SELECT id FROM users WHERE username = 'demo_nvkd')
WHERE code IN ('DL001', 'DL002') AND sales_rep_id IS NULL;
-- Đại lý demo đăng nhập cổng đặt hàng thay cho DL001 (mỗi đại lý một tài khoản)
INSERT IGNORE INTO customer_accounts (customer_id, user_id)
SELECT c.id, u.id FROM customers c JOIN users u ON u.username = 'demo_daily' WHERE c.code = 'DL001';`);
fs.writeFileSync(process.argv[2], out.join("\n") + "\n");
console.log("products", products.length, "receipts", WAREHOUSE_CASES.length * SUPPLIERS.length);
