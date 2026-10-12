package com.oms.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Một dòng nhật ký thao tác (S2-04) kèm thông tin người thực hiện để hiển thị. */
public class AuditEntry {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Mã lưu trong DB giữ tiếng Anh để truy vấn; nhãn hiển thị tiếng Việt. Mã chưa có nhãn thì hiện nguyên mã.
    private static final Map<String, String> ACTION_LABELS = Map.ofEntries(
            Map.entry("LOGIN", "Đăng nhập"),
            Map.entry("PASSWORD_CHANGE", "Đổi mật khẩu"),
            Map.entry("PASSWORD_RESET", "Đặt lại mật khẩu"),
            Map.entry("PROFILE_UPDATE", "Sửa hồ sơ cá nhân"),
            Map.entry("AVATAR_UPDATE", "Đổi ảnh đại diện"),
            Map.entry("AVATAR_REMOVE", "Xoá ảnh đại diện"),
            Map.entry("USER_CREATE", "Tạo tài khoản"),
            Map.entry("USER_UPDATE", "Sửa tài khoản"),
            Map.entry("USER_LOCK", "Khoá tài khoản"),
            Map.entry("USER_UNLOCK", "Mở khoá tài khoản"),
            Map.entry("PERMISSION_UPDATE", "Sửa phân quyền"),
            Map.entry("PRODUCT_CREATE", "Tạo sản phẩm"),
            Map.entry("PRODUCT_UPDATE", "Sửa sản phẩm"),
            Map.entry("PRODUCT_COST_UPDATE", "Sửa giá vốn"),
            Map.entry("PRODUCT_IMAGE_UPDATE", "Đổi ảnh sản phẩm"),
            Map.entry("PRODUCT_DELETE", "Xoá sản phẩm"),
            Map.entry("CATEGORY_CREATE", "Tạo nhóm hàng"),
            Map.entry("CATEGORY_RENAME", "Đổi tên nhóm hàng"),
            Map.entry("CATEGORY_DELETE", "Xoá nhóm hàng"),
            Map.entry("CATEGORY_MOVE_PRODUCTS", "Chuyển sản phẩm sang nhóm khác"),
            Map.entry("PRODUCT_UNIT_SAVE", "Khai báo quy đổi đơn vị"),
            Map.entry("PRODUCT_UNIT_DELETE", "Xoá quy đổi đơn vị"),
            Map.entry("SUPPLIER_CREATE", "Tạo nhà cung cấp"),
            Map.entry("SUPPLIER_UPDATE", "Sửa nhà cung cấp"),
            Map.entry("SUPPLIER_DELETE", "Xoá nhà cung cấp"),
            Map.entry("PRICE_LIST_CREATE", "Tạo bảng giá"),
            Map.entry("PRICE_LIST_UPDATE", "Sửa thông tin bảng giá"),
            Map.entry("PRICE_LIST_NEW_VERSION", "Tạo phiên bản bảng giá"),
            Map.entry("PRICE_ITEM_SAVE", "Sửa giá bán / giá sàn"),
            Map.entry("PRICE_ITEM_DELETE", "Xoá dòng giá"),
            Map.entry("PRODUCT_IMPORT", "Nhập sản phẩm từ Excel"),
            Map.entry("USER_IMPORT", "Nhập người dùng từ Excel"),
            Map.entry("DISCOUNT_CREATE", "Tạo chính sách chiết khấu"),
            Map.entry("DISCOUNT_UPDATE", "Sửa chính sách chiết khấu"),
            Map.entry("DISCOUNT_DELETE", "Xoá chính sách chiết khấu"),
            Map.entry("ORDER_CREATE", "Tạo đơn nháp"),
            Map.entry("ORDER_UPDATE", "Sửa thông tin giao hàng của đơn"),
            Map.entry("ORDER_DELETE", "Xoá đơn nháp"),
            Map.entry("CUSTOMER_CREATE", "Thêm đại lý"),
            Map.entry("CUSTOMER_UPDATE", "Sửa hồ sơ đại lý"),
            Map.entry("CUSTOMER_DELETE", "Xoá đại lý"),
            Map.entry("DELIVERY_POINT_SAVE", "Lưu điểm giao hàng"),
            Map.entry("DELIVERY_POINT_DEFAULT", "Đặt điểm giao mặc định"),
            Map.entry("DELIVERY_POINT_DELETE", "Xoá điểm giao hàng"),
            Map.entry("CUSTOMER_CREDIT_UPDATE", "Đổi hạn mức công nợ"),
            Map.entry("CUSTOMER_TRANSFER", "Chuyển giao đại lý"),
            Map.entry("CUSTOMER_LOCK", "Khoá giao dịch với đại lý"),
            Map.entry("CUSTOMER_UNLOCK", "Mở giao dịch với đại lý"));

    // Giá trị trước / sau của các thao tác này chứa giá vốn: chỉ người được xem giá vốn mới thấy
    private static final Set<String> COST_SENSITIVE = Set.of("PRODUCT_COST_UPDATE");

    /** Loại đối tượng dùng cho bộ lọc; thêm vào đây khi module mới ghi nhật ký. */
    public static final Map<String, String> TARGET_LABELS = new LinkedHashMap<>();

    static {
        TARGET_LABELS.put("USER", "Người dùng");
        TARGET_LABELS.put("ROLE_PERMISSIONS", "Phân quyền");
        TARGET_LABELS.put("PRODUCT", "Sản phẩm");
        TARGET_LABELS.put("CATEGORY", "Nhóm hàng");
        TARGET_LABELS.put("SUPPLIER", "Nhà cung cấp");
        TARGET_LABELS.put("PRICE_LIST", "Bảng giá");
        TARGET_LABELS.put("CUSTOMER", "Đại lý");
        TARGET_LABELS.put("ORDER", "Đơn hàng");
        TARGET_LABELS.put("DISCOUNT_POLICY", "Chính sách chiết khấu");
    }

    private long id;
    private LocalDateTime createdAt;
    private String action;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String detail;
    private String oldValue;
    private String newValue;
    private String actorName;
    private String actorUsername;
    private String actorAvatarPath;

    public boolean isCostSensitive() {
        return COST_SENSITIVE.contains(action);
    }

    public String getActionLabel() {
        return ACTION_LABELS.getOrDefault(action, action);
    }

    public String getTargetLabel() {
        return TARGET_LABELS.getOrDefault(targetType, targetType);
    }

    public String getCreatedAtText() {
        return createdAt == null ? "" : createdAt.format(TIME);
    }

    public String getActorAvatarThumbPath() {
        return actorAvatarPath == null ? null : actorAvatarPath.replaceFirst("\\.png$", "-64.png");
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getActorAvatarPath() { return actorAvatarPath; }
    public void setActorAvatarPath(String actorAvatarPath) { this.actorAvatarPath = actorAvatarPath; }
}
