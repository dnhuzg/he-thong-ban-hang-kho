package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CategoryDao;
import com.oms.model.Category;
import com.oms.model.User;

import java.sql.Connection;
import java.sql.SQLException;

/** Quản lý nhóm hàng dạng cây nhiều cấp (S2-06). */
public class CategoryService {

    private final CategoryDao categoryDao = new CategoryDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public CategoryTree tree() throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return new CategoryTree(categoryDao.listAll(conn));
        }
    }

    /** parentId = null: tạo nhóm gốc. Không giới hạn số cấp (AC yêu cầu tối thiểu ba cấp). */
    public long create(Long parentId, String rawName, User actor) throws SQLException {
        String name = validName(rawName);
        return Db.inTransaction(conn -> {
            categoryDao.lockAll(conn);
            CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
            if (parentId != null && tree.get(parentId) == null) {
                throw new ServiceException("Nhóm cha không còn tồn tại. Vui lòng tải lại trang.");
            }
            if (categoryDao.existsSibling(conn, parentId, name, 0)) {
                throw new ServiceException("Đã có nhóm \"" + name + "\" cùng cấp. Vui lòng chọn tên khác.");
            }
            long id = categoryDao.insert(conn, parentId, name);
            String path = parentId == null ? name : tree.get(parentId).getPath() + CategoryTree.SEPARATOR + name;
            auditDao.log(conn, actor.getId(), "CATEGORY_CREATE", "CATEGORY", id, null, null, path);
            return id;
        });
    }

    public void rename(long id, String rawName, User actor) throws SQLException {
        String name = validName(rawName);
        Db.inTransaction(conn -> {
            categoryDao.lockAll(conn);
            Category c = require(new CategoryTree(categoryDao.listAll(conn)), id);
            if (categoryDao.existsSibling(conn, c.getParentId(), name, id)) {
                throw new ServiceException("Đã có nhóm \"" + name + "\" cùng cấp. Vui lòng chọn tên khác.");
            }
            categoryDao.rename(conn, id, name);
            auditDao.log(conn, actor.getId(), "CATEGORY_RENAME", "CATEGORY", id, null, c.getPath(),
                    new CategoryTree(categoryDao.listAll(conn)).get(id).getPath());
            return null;
        });
    }

    /** Chỉ xoá được nhóm không còn sản phẩm và không có nhóm con. */
    public void delete(long id, User actor) throws SQLException {
        Db.inTransaction(conn -> {
            categoryDao.lockAll(conn);
            Category c = require(new CategoryTree(categoryDao.listAll(conn)), id);
            if (c.getProductCount() > 0) {
                throw new ServiceException("Nhóm \"" + c.getName() + "\" còn " + c.getProductCount()
                        + " sản phẩm nên không xoá được. Hãy chuyển sản phẩm sang nhóm khác trước.");
            }
            if (c.isHasChildren()) {
                throw new ServiceException("Nhóm \"" + c.getName() + "\" còn nhóm con nên không xoá được. Hãy xoá nhóm con trước.");
            }
            categoryDao.delete(conn, id);
            auditDao.log(conn, actor.getId(), "CATEGORY_DELETE", "CATEGORY", id, null, c.getPath(), null);
            return null;
        });
    }

    /** Chuyển mọi sản phẩm của nhóm nguồn sang nhóm đích; trả về số sản phẩm đã chuyển. */
    public int moveProducts(long fromId, long toId, User actor) throws SQLException {
        if (fromId == toId) {
            throw new ServiceException("Nhóm đích phải khác nhóm hiện tại.");
        }
        return Db.inTransaction(conn -> {
            categoryDao.lockAll(conn);
            CategoryTree tree = new CategoryTree(categoryDao.listAll(conn));
            Category from = require(tree, fromId);
            Category to = tree.get(toId);
            if (to == null) {
                throw new ServiceException("Vui lòng chọn nhóm đích.");
            }
            int moved = categoryDao.moveProducts(conn, fromId, toId);
            if (moved == 0) {
                throw new ServiceException("Nhóm \"" + from.getName() + "\" không có sản phẩm nào để chuyển.");
            }
            auditDao.log(conn, actor.getId(), "CATEGORY_MOVE_PRODUCTS", "CATEGORY", fromId, moved + " sản phẩm",
                    from.getPath(), to.getPath());
            return moved;
        });
    }

    private static Category require(CategoryTree tree, long id) {
        Category c = tree.get(id);
        if (c == null) {
            throw new ServiceException("Nhóm hàng không còn tồn tại. Vui lòng tải lại trang.");
        }
        return c;
    }

    private static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tên nhóm hàng.");
        }
        if (name.length() > 150) {
            throw new ServiceException("Tên nhóm hàng tối đa 150 ký tự.");
        }
        return name;
    }
}
