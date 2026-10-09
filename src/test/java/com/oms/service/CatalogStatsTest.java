package com.oms.service;

import com.oms.model.CatalogStats;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Số liệu đầu trang Sản phẩm: chỉ tính bảng giá đang hiệu lực khi đếm sản phẩm chưa có giá. */
class CatalogStatsTest extends DatabaseTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 3, 15);

    @Test
    void countsProductsAndThoseWithoutAnEffectivePrice() throws Exception {
        sql("INSERT INTO products (id, sku, name, category_id, base_unit_id, status, created_at) VALUES "
                + "(500, 'COCA', 'Coca', 3, 1, 'ACTIVE', TIMESTAMP '2027-03-02 08:00:00'), "
                + "(501, 'PEPSI', 'Pepsi', 3, 1, 'ACTIVE', TIMESTAMP '2027-02-10 08:00:00'), "
                + "(502, 'FANTA', 'Fanta', 3, 1, 'ACTIVE', TIMESTAMP '2027-01-10 08:00:00'), "
                + "(503, 'SPRITE', 'Sprite', 3, 1, 'DISCONTINUED', TIMESTAMP '2027-01-10 08:00:00')");
        sql("INSERT INTO price_lists (id, code, version, name, customer_group_id, valid_from, valid_to) VALUES "
                + "(1, 'BG1', 1, 'Đang hiệu lực', 1, DATE '2027-01-01', NULL), "
                + "(2, 'BG2', 1, 'Đã hết hạn', 2, DATE '2026-01-01', DATE '2026-12-31')");
        sql("INSERT INTO price_list_items (price_list_id, product_id, sale_price, floor_price) VALUES (1, 500, 10000, 9000), (2, 501, 10000, 9000)");

        CatalogStats stats = new ProductService().catalogStats(TODAY);

        assertEquals(4, stats.getTotalProducts());
        assertEquals(3, stats.getActiveProducts());
        assertEquals(1, stats.getDiscontinuedProducts());
        assertEquals(1, stats.getNewThisMonth());
        assertEquals(1, stats.getEffectivePriceLists());
        // PEPSI chỉ có giá trong bảng đã hết hạn, FANTA chưa có giá; SPRITE ngừng bán nên không tính
        assertEquals(2, stats.getActiveWithoutPrice());
    }
}
