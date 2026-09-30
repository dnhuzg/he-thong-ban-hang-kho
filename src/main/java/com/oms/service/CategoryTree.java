package com.oms.service;

import com.oms.model.Category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dựng cây nhóm hàng từ danh sách phẳng (parent_id): thứ tự duyệt theo chiều sâu, cấp và đường dẫn đầy đủ.
 * Dùng cho ô chọn nhóm hàng, lọc sản phẩm theo cả nhóm con và trang quản lý nhóm hàng.
 */
public final class CategoryTree {

    public static final String SEPARATOR = " › ";

    private final List<Category> ordered = new ArrayList<>();
    private final Map<Long, Category> byId = new HashMap<>();
    private final Map<Long, List<Category>> children = new HashMap<>();

    public CategoryTree(List<Category> flat) {
        for (Category c : flat) {
            byId.put(c.getId(), c);
        }
        for (Category c : flat) {
            // Cha không tồn tại thì coi như gốc, tránh mất nhánh khi dữ liệu lệch
            Long parent = c.getParentId() != null && byId.containsKey(c.getParentId()) ? c.getParentId() : null;
            children.computeIfAbsent(parent, k -> new ArrayList<>()).add(c);
        }
        children.values().forEach(list -> list.sort(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER)));
        walk(null, 1, "", new HashSet<>());
        for (Category c : ordered) {
            c.setHasChildren(hasChildren(c.getId()));
            c.setTotalProductCount(selfAndDescendants(c.getId()).stream()
                    .mapToLong(id -> byId.get(id).getProductCount()).sum());
        }
    }

    private void walk(Long parentId, int level, String prefix, Set<Long> seen) {
        for (Category c : children.getOrDefault(parentId, List.of())) {
            if (!seen.add(c.getId())) {
                continue; // chặn vòng lặp cha–con nếu dữ liệu hỏng
            }
            c.setLevel(level);
            c.setPath(prefix.isEmpty() ? c.getName() : prefix + SEPARATOR + c.getName());
            ordered.add(c);
            walk(c.getId(), level + 1, c.getPath(), seen);
        }
    }

    /** Toàn bộ nhóm theo thứ tự cây (cha trước, con sau). */
    public List<Category> ordered() {
        return ordered;
    }

    public Category get(long id) {
        return byId.get(id);
    }

    /** Nhóm và mọi nhóm con cháu của nó. */
    public List<Long> selfAndDescendants(long id) {
        List<Long> ids = new ArrayList<>();
        collect(id, ids);
        return ids;
    }

    private void collect(long id, List<Long> ids) {
        if (!byId.containsKey(id) || ids.contains(id)) {
            return;
        }
        ids.add(id);
        for (Category child : children.getOrDefault(id, List.of())) {
            collect(child.getId(), ids);
        }
    }

    public boolean hasChildren(long id) {
        return !children.getOrDefault(id, List.of()).isEmpty();
    }
}
