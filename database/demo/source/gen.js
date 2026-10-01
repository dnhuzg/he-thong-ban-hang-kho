// Sinh database/demo/demo_catalog.sql và demo_catalog_remove.sql từ data.js. Cần Node.js, chỉ khi sửa dữ liệu nguồn:
//   node database/demo/source/gen.js
const fs = require("fs");
const path = require("path");
const { NEW_UNITS, TREE, BLOCKS } = require("./data.js");

const EXISTING_UNITS = { LON: "lon", LOC: "lốc", THUNG: "thùng", CHAI: "chai", GOI: "gói", HOP: "hộp", KG: "kg", CAI: "cái" };
const UNIT_NAMES = { ...EXISTING_UNITS };
NEW_UNITS.forEach(([code, name]) => (UNIT_NAMES[code] = name.toLowerCase()));
// nhóm đã có sẵn trong DB trước khi thêm dữ liệu demo: không xoá khi gỡ demo
const EXISTING_CATS = new Set(["Đồ uống", "Đồ uống/Nước giải khát", "Đồ uống/Nước giải khát/Nước ngọt có ga", "Đồ uống/Nước giải khát/Nước tăng lực", "Đồ uống/Bia", "Thực phẩm khô", "Thực phẩm khô/Mì, phở ăn liền"]);

const q = (s) => "'" + String(s).replace(/\\/g, "\\\\").replace(/'/g, "''") + "'";

// --- cây nhóm: danh sách đường dẫn theo cấp
const cats = []; // {path, parts}
const validPaths = new Set();
for (const [c1, l2] of Object.entries(TREE)) {
  cats.push([c1]); validPaths.add(c1);
  for (const [c2, l3] of Object.entries(l2)) {
    cats.push([c1, c2]); validPaths.add(c1 + "/" + c2);
    for (const c3 of l3) { cats.push([c1, c2, c3]); validPaths.add(c1 + "/" + c2 + "/" + c3); }
  }
}

// --- sản phẩm
const products = [];
const skus = new Set(), names = new Set();
const EXISTING_SKUS = ["COCA-330", "PEPSI-330", "REDBULL-250", "TIGER-330", "HAOHAO-TOM", "KO"];
const EXISTING_NAMES = ["Coca-Cola lon 330ml", "Pepsi lon 330ml", "Red Bull lon 250ml", "Bia Tiger lon 330ml", "Mì Hảo Hảo tôm chua cay", "Mì Kokomi"];
EXISTING_SKUS.forEach((s) => skus.add(s.toLowerCase()));
EXISTING_NAMES.forEach((s) => names.add(s.toLowerCase()));
const parseConv = (s) => (s ? s.split(",").map((t) => { const m = t.match(/^([A-Z]+)(\d+)$/); if (!m) throw new Error("conv sai: " + t); return [m[1], Number(m[2])]; }) : []);
for (const [path, base, conv, items] of BLOCKS) {
  if (!validPaths.has(path)) throw new Error("nhóm không có trong cây: " + path);
  if (!UNIT_NAMES[base]) throw new Error("ĐVT lạ: " + base);
  for (const [sku, name, cost, o = {}] of items) {
    if (skus.has(sku.toLowerCase())) throw new Error("SKU trùng: " + sku);
    if (names.has(name.toLowerCase())) throw new Error("Tên trùng: " + name);
    skus.add(sku.toLowerCase()); names.add(name.toLowerCase());
    const u = o.u || base;
    const cv = parseConv(o.c || conv).filter(([unit]) => unit !== u);
    cv.forEach(([unit]) => { if (!UNIT_NAMES[unit]) throw new Error("ĐVT quy đổi lạ: " + unit + " ở " + sku); });
    if (sku.length > 50 || name.length > 255) throw new Error("quá dài: " + sku);
    const big = cv.find(([unit]) => unit === "THUNG") || cv.find(([unit]) => unit === "LOC") || cv[0];
    const packing = big ? `${big[1]} ${UNIT_NAMES[u]}/${UNIT_NAMES[big[0]]}` : null;
    products.push({ sku, name, path, parts: path.split("/"), base: u, packing, cost, status: o.x ? "DISCONTINUED" : "ACTIVE", conv: cv });
  }
}
if (cv2(products)) throw new Error("quy đổi quá 2 đơn vị");
function cv2(ps) { return ps.some((p) => p.conv.length > 2); }

// --- sinh SQL cài demo
const out = [];
out.push(`-- Dữ liệu DEMO: danh mục nhóm hàng nhiều cấp và sản phẩm FMCG để thử giao diện, tìm kiếm, lọc, phân trang.
-- Nguồn: file này được sinh từ danh sách có cấu trúc; chạy lại nhiều lần an toàn (chỉ thêm cái chưa có, không đổi gì đã có).
-- Giữ nguyên nhóm hàng và sản phẩm hiện có. Không phải migration: chỉ chạy khi cần dữ liệu thử.
-- Gỡ: database/demo/demo_catalog_remove.sql.
-- Chạy:  Get-Content database\\demo\\demo_catalog.sql -Raw -Encoding UTF8 | mysql -uoms_app --default-character-set=utf8mb4
USE oms;
`);
out.push("-- 1. Đơn vị tính mới (đơn vị cũ giữ nguyên)");
for (const [code, name] of NEW_UNITS) out.push(`INSERT INTO units (code, name) SELECT ${q(code)}, ${q(name)} FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM units WHERE code = ${q(code)});`);
out.push("\n-- 2. Nhóm hàng (cấp 1 → cấp 2 → cấp 3), bỏ qua nhóm đã có cùng tên cùng cha");
for (const parts of cats) {
  const [c1, c2, c3] = parts;
  if (parts.length === 1) {
    out.push(`INSERT INTO product_categories (parent_id, name) SELECT NULL, ${q(c1)} FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE parent_id IS NULL AND name = ${q(c1)});`);
  } else if (parts.length === 2) {
    out.push(`INSERT INTO product_categories (parent_id, name) SELECT p.id, ${q(c2)} FROM product_categories p WHERE p.parent_id IS NULL AND p.name = ${q(c1)} AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p.id AND c.name = ${q(c2)});`);
  } else {
    out.push(`INSERT INTO product_categories (parent_id, name) SELECT p2.id, ${q(c3)} FROM product_categories p2 JOIN product_categories p1 ON p1.id = p2.parent_id WHERE p1.parent_id IS NULL AND p1.name = ${q(c1)} AND p2.name = ${q(c2)} AND NOT EXISTS (SELECT 1 FROM product_categories c WHERE c.parent_id = p2.id AND c.name = ${q(c3)});`);
  }
}
out.push(`
-- 3. Sản phẩm: nạp vào bảng tạm rồi gắn nhóm hàng và đơn vị theo tên
DROP TEMPORARY TABLE IF EXISTS tmp_demo_products;
CREATE TEMPORARY TABLE tmp_demo_products (
  sku VARCHAR(50) NOT NULL, name VARCHAR(255) NOT NULL,
  cat1 VARCHAR(150) NOT NULL, cat2 VARCHAR(150) NULL, cat3 VARCHAR(150) NULL,
  base_unit VARCHAR(20) NOT NULL, packing_spec VARCHAR(255) NULL, cost_price DECIMAL(15,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  conv1_unit VARCHAR(20) NULL, conv1_factor DECIMAL(12,4) NULL, conv2_unit VARCHAR(20) NULL, conv2_factor DECIMAL(12,4) NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
`);
const nul = (v) => (v == null ? "NULL" : v);
let lastPath = "";
const rows = [];
for (const p of products) {
  if (p.path !== lastPath) { rows.push(`-- ${p.path}`); lastPath = p.path; }
  const [c1, c2, c3] = p.parts;
  const [a, b] = p.conv;
  rows.push(`(${q(p.sku)}, ${q(p.name)}, ${q(c1)}, ${c2 ? q(c2) : "NULL"}, ${c3 ? q(c3) : "NULL"}, ${q(p.base)}, ${p.packing ? q(p.packing) : "NULL"}, ${p.cost}, ${q(p.status)}, ${a ? q(a[0]) : "NULL"}, ${nul(a && a[1])}, ${b ? q(b[0]) : "NULL"}, ${nul(b && b[1])})`);
}
// chia thành nhiều lệnh INSERT, mỗi lệnh gắn nhãn nhóm bằng dòng chú thích
out.push("INSERT INTO tmp_demo_products (sku, name, cat1, cat2, cat3, base_unit, packing_spec, cost_price, status, conv1_unit, conv1_factor, conv2_unit, conv2_factor) VALUES");
const valueLines = rows.map((r, i) => r);
let body = "";
const dataRows = rows.filter((r) => !r.startsWith("--"));
let n = 0;
for (const r of rows) {
  if (r.startsWith("--")) { body += r + "\n"; continue; }
  n++;
  body += r + (n === dataRows.length ? ";\n" : ",\n");
}
out.push(body.trimEnd());
out.push(`
INSERT INTO products (sku, name, category_id, base_unit_id, packing_spec, cost_price, status)
SELECT t.sku, t.name, COALESCE(c3.id, c2.id, c1.id), bu.id, t.packing_spec, t.cost_price, t.status
FROM tmp_demo_products t
JOIN units bu ON bu.code = t.base_unit
JOIN product_categories c1 ON c1.parent_id IS NULL AND c1.name = t.cat1
LEFT JOIN product_categories c2 ON c2.parent_id = c1.id AND c2.name = t.cat2
LEFT JOIN product_categories c3 ON c3.parent_id = c2.id AND c3.name = t.cat3
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.sku = t.sku);

-- 4. Quy đổi đơn vị (lốc, thùng...) cho các sản phẩm demo
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, t.conv1_factor FROM tmp_demo_products t
JOIN products p ON p.sku = t.sku JOIN units u ON u.code = t.conv1_unit WHERE t.conv1_unit IS NOT NULL;
INSERT IGNORE INTO product_units (product_id, unit_id, factor)
SELECT p.id, u.id, t.conv2_factor FROM tmp_demo_products t
JOIN products p ON p.sku = t.sku JOIN units u ON u.code = t.conv2_unit WHERE t.conv2_unit IS NOT NULL;

DROP TEMPORARY TABLE tmp_demo_products;
`);
fs.writeFileSync(path.join(__dirname, "..", "demo_catalog.sql"), out.join("\n") + "\n");

// --- sinh SQL gỡ demo
const rm = [];
rm.push(`-- Gỡ dữ liệu DEMO do demo_catalog.sql thêm vào: xoá sản phẩm demo, rồi các nhóm hàng demo còn trống (không có sản phẩm, không có nhóm con).
-- Sản phẩm / nhóm đã được dùng ở nơi khác (bảng giá, đơn hàng...) bị khoá ngoại giữ lại, không bị xoá. Nhóm hàng và sản phẩm có từ trước không bị đụng tới.
-- Đơn vị tính mới thêm (Tuýp, Túi, Bịch...) được giữ lại.
USE oms;
`);
rm.push("DELETE IGNORE FROM products WHERE sku IN (" + products.map((p) => q(p.sku)).join(", ") + ");");
rm.push("");
const demoOnly = cats.filter((p) => !EXISTING_CATS.has(p.join("/")));
for (const level of [3, 2, 1]) {
  rm.push(`-- nhóm cấp ${level} của demo (xoá khi đã trống)`);
  for (const parts of demoOnly.filter((p) => p.length === level)) {
    const [c1, c2, c3] = parts;
    const join = level === 1 ? "" : level === 2 ? ` AND c.parent_id = (SELECT id FROM (SELECT id FROM product_categories WHERE parent_id IS NULL AND name = ${q(c1)}) x)` : ` AND c.parent_id = (SELECT id FROM (SELECT l2.id FROM product_categories l2 JOIN product_categories l1 ON l1.id = l2.parent_id WHERE l1.parent_id IS NULL AND l1.name = ${q(c1)} AND l2.name = ${q(c2)}) x)`;
    const name = parts[parts.length - 1];
    const parentCond = level === 1 ? " AND c.parent_id IS NULL" : join;
    rm.push(`DELETE IGNORE c FROM product_categories c WHERE c.name = ${q(name)}${parentCond} AND NOT EXISTS (SELECT 1 FROM (SELECT parent_id FROM product_categories) k WHERE k.parent_id = c.id) AND NOT EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.id);`);
  }
}
fs.writeFileSync(path.join(__dirname, "..", "demo_catalog_remove.sql"), rm.join("\n") + "\n");

// --- thống kê
const l1 = cats.filter((c) => c.length === 1).length, l2 = cats.filter((c) => c.length === 2).length, l3 = cats.filter((c) => c.length === 3).length;
const act = products.filter((p) => p.status === "ACTIVE").length;
console.log(JSON.stringify({ cap1: l1, cap2: l2, cap3: l3, tongNhom: cats.length, sanPham: products.length, dangKinhDoanh: act, ngungKinhDoanh: products.length - act, demoOnlyCats: demoOnly.length }));
