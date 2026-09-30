package com.oms.service;

import com.oms.model.Category;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-06: cây nhóm hàng tối thiểu ba cấp, chuyển sản phẩm giữa nhóm, nhóm còn sản phẩm không xoá được. */
class CategoryServiceTest extends DatabaseTest {

    private final CategoryService service = new CategoryService();

    @Test
    void buildsTreeDeeperThanThreeLevelsWithFullPath() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        // Có sẵn 1 → 2 → 3; thêm cấp 4 và 5
        long level4 = service.create(3L, "Lon nhỏ", manager);
        long level5 = service.create(level4, "Loại 250ml", manager);

        Category c = service.tree().get(level5);
        assertEquals(5, c.getLevel());
        assertEquals("Đồ uống › Nước giải khát › Nước ngọt có ga › Lon nhỏ › Loại 250ml", c.getPath());
        assertEquals(1, count("audit_logs", "action = 'CATEGORY_CREATE' AND target_id = " + level5));
    }

    @Test
    void siblingNamesMustBeUniqueButSameNameUnderOtherParentIsAllowed() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        service.create(1L, "Bia", manager);
        assertThrows(ServiceException.class, () -> service.create(1L, "bia", manager));
        service.create(4L, "Bia", manager);   // nhóm cha khác
        long other = service.create(1L, "Rượu", manager);
        assertThrows(ServiceException.class, () -> service.rename(other, "BIA", manager));
    }

    @Test
    void cannotDeleteCategoryWithProductsOrChildren() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        sql("INSERT INTO products (sku, name, category_id, base_unit_id) VALUES ('COCA', 'Coca', 3, 1)");

        ServiceException withProducts = assertThrows(ServiceException.class, () -> service.delete(3, manager));
        assertTrue(withProducts.getMessage().contains("còn 1 sản phẩm"));
        ServiceException withChildren = assertThrows(ServiceException.class, () -> service.delete(2, manager));
        assertTrue(withChildren.getMessage().contains("nhóm con"));

        long empty = service.create(4L, "Nhóm trống", manager);
        service.delete(empty, manager);
        assertEquals(0, count("product_categories", "id = " + empty));
    }

    @Test
    void movesProductsBetweenCategoriesThenSourceCanBeDeleted() throws Exception {
        User manager = user("qlkd", Role.SALES_MANAGER);
        long source = service.create(4L, "Mì cũ", manager);
        sql("INSERT INTO products (sku, name, category_id, base_unit_id) VALUES ('MI1', 'Mì 1', " + source + ", 3), ('MI2', 'Mì 2', " + source + ", 3)");

        assertEquals(2, service.moveProducts(source, 4, manager));
        assertEquals(2, count("products", "category_id = 4"));
        assertThrows(ServiceException.class, () -> service.moveProducts(source, 4, manager));  // hết sản phẩm để chuyển
        assertThrows(ServiceException.class, () -> service.moveProducts(4, 4, manager));
        service.delete(source, manager);
        assertEquals(1, count("audit_logs", "action = 'CATEGORY_MOVE_PRODUCTS'"));
    }

    @Test
    void totalCountsIncludeDescendants() throws Exception {
        sql("INSERT INTO products (sku, name, category_id, base_unit_id) VALUES ('A', 'A', 3, 1), ('B', 'B', 2, 1), ('C', 'C', 4, 1)");
        CategoryTree tree = service.tree();
        assertEquals(2, tree.get(1).getTotalProductCount());
        assertEquals(0, tree.get(1).getProductCount());
        assertTrue(tree.get(1).isHasChildren());
        assertFalse(tree.get(3).isHasChildren());
    }
}
