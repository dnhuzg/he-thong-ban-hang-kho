package com.oms.service;

import com.oms.model.Product;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-05: SKU duy nhất, giá vốn chỉ Quản lý kinh doanh xem / sửa, đã phát sinh dữ liệu thì không xoá được. */
class ProductServiceTest extends DatabaseTest {

    @TempDir
    Path uploads;

    private ProductService service() {
        return new ProductService(new FileStorage(uploads));
    }

    private static ProductForm form(String sku, String cost) {
        ProductForm f = new ProductForm();
        f.setSku(sku);
        f.setName("Coca-Cola lon 330ml");
        f.setCategoryId(3);
        f.setBaseUnitId(1);
        f.setPackingSpec("24 lon/thùng");
        f.setCostPrice(cost);
        f.setStatus("ACTIVE");
        return f;
    }

    @Test
    void managerCreatesProductWithNormalizedSkuAndCost() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        long id = service().create(form(" coca-330 ", "7.200"), null, manager);

        Product p = service().get(id, manager);
        assertEquals("COCA-330", p.getSku());
        assertEquals(0, new BigDecimal("7200").compareTo(p.getCostPrice()));
        assertEquals("Đồ uống › Nước giải khát › Nước ngọt có ga", p.getCategoryPath());
        assertEquals(1, count("audit_logs", "action = 'PRODUCT_COST_UPDATE'"));
    }

    @Test
    void skuMustBeUnique() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        service().create(form("COCA-330", ""), null, manager);
        ServiceException e = assertThrows(ServiceException.class, () -> service().create(form("coca-330", ""), null, manager));
        assertTrue(e.getMessage().contains("đã tồn tại"));
    }

    @Test
    void costPriceIsHiddenAndIgnoredForOtherRoles() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        User admin = user("admin01", Role.ADMIN);
        User rep = user("rep01", Role.SALES_REP);
        User keeper = user("kho01", Role.WAREHOUSE);
        long id = service().create(form("COCA-330", "7200"), null, manager);

        // Người không có quyền xem giá vốn: DAO không đọc giá vốn ra
        assertNull(service().get(id, rep).getCostPrice());
        assertNull(service().get(id, keeper).getCostPrice());
        assertNull(service().search("", 0, null, 1, rep).getItems().get(0).getCostPrice());

        // Admin sửa sản phẩm bình thường; kho gửi kèm giá vốn cũng bị bỏ qua
        ProductForm change = form("COCA-330", "1");
        change.setId(id);
        service().update(change, null, false, keeper);
        assertEquals(0, new BigDecimal("7200").compareTo(service().get(id, manager).getCostPrice()));
        change.setCostPrice("7500");
        service().update(change, null, false, admin);
        assertEquals(0, new BigDecimal("7500").compareTo(service().get(id, manager).getCostPrice()));
    }

    @Test
    void filterByCategoryIncludesSubcategories() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        service().create(form("COCA-330", ""), null, manager);   // nhóm 3 (con cháu của 1)
        ProductForm noodle = form("HAOHAO", "");
        noodle.setCategoryId(4);
        noodle.setBaseUnitId(3);
        service().create(noodle, null, manager);

        assertEquals(1, service().search("", 1, null, 1, manager).getTotal());
        assertEquals(1, service().search("", 4, null, 1, manager).getTotal());
        assertEquals(2, service().search("", 0, null, 1, manager).getTotal());
        assertEquals(1, service().search("HAO", 0, null, 1, manager).getTotal());
    }

    @Test
    void cannotDeleteProductReferencedByOtherDataButCanDiscontinue() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        long id = service().create(form("COCA-330", ""), null, manager);
        // Mô phỏng một giao dịch đã tham chiếu sản phẩm (bảng đơn hàng / phiếu kho sau này)
        sql("CREATE TABLE fake_order_lines (id BIGINT PRIMARY KEY, product_id BIGINT REFERENCES products(id))");
        sql("INSERT INTO fake_order_lines VALUES (1, " + id + ")");

        ServiceException e = assertThrows(ServiceException.class, () -> service().delete(id, manager));
        assertTrue(e.getMessage().contains("Ngừng kinh doanh"));
        assertEquals(1, count("products", "id = " + id));
        assertEquals(0, count("audit_logs", "action = 'PRODUCT_DELETE'"));

        ProductForm stop = form("COCA-330", "");
        stop.setId(id);
        stop.setStatus("DISCONTINUED");
        service().update(stop, null, false, manager);
        assertEquals(Product.Status.DISCONTINUED, service().get(id, manager).getStatus());
    }

    @Test
    void deletesUnusedProduct() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        long id = service().create(form("COCA-330", ""), null, manager);
        service().delete(id, manager);
        assertEquals(0, count("products", "id = " + id));
    }

    @Test
    void validatesInput() {
        assertThrows(ServiceException.class, () -> ProductService.normalizeSku("có dấu"));
        assertThrows(ServiceException.class, () -> ProductService.normalizeSku("A"));
        assertThrows(ServiceException.class, () -> ProductService.parseCost("-5"));
        assertThrows(ServiceException.class, () -> ProductService.parseCost("12.3456")); // quá 2 chữ số thập phân
        assertEquals(0, new BigDecimal("12345").compareTo(ProductService.parseCost("12.345"))); // dấu chấm hàng nghìn
        assertThrows(ServiceException.class, () -> ProductService.parseCost("abc"));
        assertEquals(0, new BigDecimal("1250000").compareTo(ProductService.parseCost("1.250.000")));
        assertEquals(0, new BigDecimal("7200.50").compareTo(ProductService.parseCost("7200.50")));
        assertNull(ProductService.parseCost(""));
    }
}
