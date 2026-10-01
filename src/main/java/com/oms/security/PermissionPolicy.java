package com.oms.security;

import com.oms.model.Module;
import com.oms.model.PermissionLevel;
import com.oms.model.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Quyết định quyền từ ma trận vai trò × module. Không đụng DB để kiểm thử được.
 * Mặc định từ chối: module không có trong ma trận của vai trò nào thì không truy cập (S1-05).
 */
public class PermissionPolicy {

    private final Map<Role, Map<Module, PermissionLevel>> matrix;

    public PermissionPolicy(Map<Role, Map<Module, PermissionLevel>> matrix) {
        this.matrix = matrix;
    }

    public PermissionLevel levelOf(Role role, Module module) {
        if (role == Role.ADMIN) {
            return PermissionLevel.F;
        }
        return matrix.getOrDefault(role, Collections.emptyMap()).getOrDefault(module, PermissionLevel.NONE);
    }

    public boolean canRead(Set<Role> roles, Module module) {
        for (Role r : roles) {
            if (levelOf(r, module).canRead()) {
                return true;
            }
        }
        return false;
    }

    public boolean canWrite(Set<Role> roles, Module module) {
        for (Role r : roles) {
            if (levelOf(r, module).canWrite()) {
                return true;
            }
        }
        return false;
    }

    /** Chỉ thấy dữ liệu của mình khi không vai trò nào cho phép xem toàn bộ. */
    public boolean isOwnScopeOnly(Set<Role> roles, Module module) {
        boolean any = false;
        for (Role r : roles) {
            PermissionLevel level = levelOf(r, module);
            if (level.canRead()) {
                any = true;
                if (!level.isOwnOnly()) {
                    return false;
                }
            }
        }
        return any;
    }

    /** Ràng buộc cố định của dự án, không phụ thuộc ma trận: giá vốn / biên lợi nhuận chỉ Quản lý kinh doanh (và Admin). */
    public static boolean canViewCostPrice(Set<Role> roles) {
        return roles.contains(Role.SALES_MANAGER) || roles.contains(Role.ADMIN);
    }

    /**
     * Ràng buộc cố định: hồ sơ đại lý do Admin, Quản lý kinh doanh và Kế toán công nợ sửa. Nhân viên kinh doanh chỉ
     * xem đại lý mình phụ trách (ma trận cho W_OWN nên không dùng canWrite để chặn việc sửa hồ sơ).
     */
    public static boolean canManageCustomers(Set<Role> roles) {
        return roles.contains(Role.ADMIN) || roles.contains(Role.SALES_MANAGER) || roles.contains(Role.ACCOUNTANT);
    }

    /** Ràng buộc cố định: phân công và chuyển giao người phụ trách đại lý chỉ Quản lý kinh doanh (và Admin). */
    public static boolean canAssignCustomers(Set<Role> roles) {
        return roles.contains(Role.SALES_MANAGER) || roles.contains(Role.ADMIN);
    }

    /** Ràng buộc cố định: hạn mức công nợ và số ngày nợ chỉ Kế toán công nợ, Quản lý kinh doanh (và Admin) sửa. */
    public static boolean canManageCredit(Set<Role> roles) {
        return roles.contains(Role.ACCOUNTANT) || roles.contains(Role.SALES_MANAGER) || roles.contains(Role.ADMIN);
    }

    /** Ràng buộc cố định: chính sách chiết khấu theo sản lượng chỉ Quản lý kinh doanh (và Admin) khai báo. */
    public static boolean canManageDiscounts(Set<Role> roles) {
        return roles.contains(Role.SALES_MANAGER) || roles.contains(Role.ADMIN);
    }

    /** Lập đơn hàng cho đại lý: Nhân viên kinh doanh, Quản lý kinh doanh (và Admin). Kho, kế toán chưa có việc với đơn nháp. */
    public static boolean canTakeOrders(Set<Role> roles) {
        return roles.contains(Role.SALES_REP) || canSeeAllOrders(roles);
    }

    /** Nhân viên kinh doanh chỉ thấy đơn của đại lý mình phụ trách; Quản lý kinh doanh (và Admin) thấy tất cả. */
    public static boolean canSeeAllOrders(Set<Role> roles) {
        return roles.contains(Role.SALES_MANAGER) || roles.contains(Role.ADMIN);
    }

    /** Đại lý (tài khoản khách) chưa có cổng riêng nên không xem được danh sách đại lý; kho cũng không có quyền xem. */
    public static boolean canBrowseCustomers(Set<Role> roles) {
        return canManageCustomers(roles) || roles.contains(Role.SALES_REP);
    }

    /** Trang cấu hình phân quyền chỉ dành cho Quản trị hệ thống. */
    public static boolean canManagePermissions(Set<Role> roles) {
        return roles.contains(Role.ADMIN);
    }

    public Map<Role, Map<Module, PermissionLevel>> asMatrix() {
        Map<Role, Map<Module, PermissionLevel>> copy = new EnumMap<>(Role.class);
        for (Role r : Role.configurable()) {
            Map<Module, PermissionLevel> row = new EnumMap<>(Module.class);
            for (Module m : Module.values()) {
                row.put(m, levelOf(r, m));
            }
            copy.put(r, row);
        }
        return copy;
    }
}
