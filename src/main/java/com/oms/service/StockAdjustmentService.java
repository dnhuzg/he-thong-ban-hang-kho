package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.InventoryLotDao;
import com.oms.dao.InvoiceDao;
import com.oms.dao.StockAdjustmentDao;
import com.oms.dao.StockAdjustmentDao.Adjustment;
import com.oms.dao.StockAdjustmentDao.StockOption;
import com.oms.model.Role;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phiếu điều chỉnh giảm tồn (S8-03): hàng hỏng, mất, hết hạn phải huỷ. Nhân viên kho hoặc quản lý kho lập; chỉ
 * Quản lý kho duyệt và không được duyệt phiếu do chính mình lập. Duyệt mới trừ tồn và ghi bút toán thẻ kho.
 */
public class StockAdjustmentService {

    private static final int LIST_LIMIT = 100;
    public static final Map<String, String> REASONS = Map.of(
            "DAMAGED", "Hàng hỏng", "LOST", "Hàng mất", "EXPIRED", "Hết hạn phải huỷ");

    private final StockAdjustmentDao dao = new StockAdjustmentDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public static class NotFoundException extends RuntimeException {
    }

    public record Form(long warehouseId, long productId, Long lotId, BigDecimal quantity, String reason, String note) {
    }

    public record Page(List<Adjustment> adjustments, List<StockOption> options, List<InventoryLotDao.Lot> lots,
                       boolean canApprove, long viewerId) {
        public List<Adjustment> getAdjustments() { return adjustments; }
        public List<StockOption> getOptions() { return options; }
        public List<InventoryLotDao.Lot> getLots() { return lots; }
        public boolean isCanApprove() { return canApprove; }
        public long getViewerId() { return viewerId; }
        public Map<String, String> getReasons() { return REASONS; }
    }

    public Page page(User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Set<Long> scope = scope(conn, viewer);
            List<InventoryLotDao.Lot> lots = new InventoryLotDao().lots(conn, null, null, false).stream()
                    .filter(l -> l.tracked() && l.remaining().signum() > 0 && (scope == null || scope.contains(l.warehouseId())))
                    .toList();
            return new Page(dao.list(conn, scope, LIST_LIMIT), dao.stockOptions(conn, scope), lots,
                    canApprove(viewer), viewer.getId());
        }
    }

    public long create(Form form, User actor) throws SQLException {
        if (!REASONS.containsKey(form.reason())) {
            throw new ServiceException("Chọn lý do điều chỉnh: hàng hỏng, hàng mất hoặc hết hạn phải huỷ.");
        }
        String note = form.note() == null ? "" : form.note().strip();
        if (note.isEmpty() || note.length() > 500) {
            throw new ServiceException("Ghi rõ diễn giải (tối đa 500 ký tự).");
        }
        if (form.quantity() == null || form.quantity().signum() <= 0) {
            throw new ServiceException("Số lượng điều chỉnh phải lớn hơn 0.");
        }
        return Db.inTransaction(conn -> {
            Set<Long> scope = scope(conn, actor);
            if (scope != null && !scope.contains(form.warehouseId())) {
                throw new ForbiddenException("Bạn không phụ trách kho này.");
            }
            StockOption stock = dao.stock(conn, form.warehouseId(), form.productId());
            if (stock == null || stock.available().compareTo(form.quantity()) < 0) {
                throw new ServiceException("Số lượng vượt tồn khả dụng của SKU tại kho.");
            }
            if (stock.tracked() && form.lotId() == null) {
                throw new ServiceException("SKU " + stock.sku() + " quản lý theo lô: chọn lô cần điều chỉnh.");
            }
            Long lotId = stock.tracked() ? form.lotId() : null;
            if (lotId != null && new InventoryLotDao().lots(conn, form.productId(), form.warehouseId(), false).stream()
                    .noneMatch(l -> l.id() == lotId && l.remaining().compareTo(form.quantity()) >= 0)) {
                throw new ServiceException("Lô đã chọn không thuộc SKU / kho này hoặc không còn đủ số lượng.");
            }
            String code = String.format("DC%06d", new InvoiceDao().nextNumber(conn, "STOCK_ADJUSTMENT"));
            long id = dao.insert(conn, code, form.warehouseId(), form.productId(), lotId, form.quantity(), form.reason(),
                    note, stock.costPrice(), actor.getId());
            auditDao.log(conn, actor.getId(), "STOCK_ADJUSTMENT_CREATE", "STOCK_ADJUSTMENT", id,
                    code + " · " + stock.sku() + " · " + form.quantity().stripTrailingZeros().toPlainString() + " · "
                            + REASONS.get(form.reason()));
            return id;
        });
    }

    public void approve(long id, User actor) throws SQLException {
        Db.inTransaction(conn -> {
            Adjustment adjustment = pending(conn, id, actor);
            if (!dao.deduct(conn, adjustment.warehouseId(), adjustment.productId(), adjustment.quantity())) {
                throw new ServiceException("Tồn khả dụng không còn đủ để điều chỉnh " + adjustment.code() + ".");
            }
            if (adjustment.lotId() != null && !dao.deductLot(conn, adjustment.lotId(), adjustment.warehouseId(),
                    adjustment.productId(), adjustment.quantity())) {
                throw new ServiceException("Lô " + adjustment.lotNo() + " không còn đủ số lượng.");
            }
            dao.insertMovement(conn, adjustment.warehouseId(), adjustment.productId(), adjustment.quantity(), id, actor.getId());
            dao.decide(conn, id, "APPROVED", null, actor.getId());
            auditDao.log(conn, actor.getId(), "STOCK_ADJUSTMENT_APPROVE", "STOCK_ADJUSTMENT", id,
                    adjustment.code() + " · tổn thất " + adjustment.lossValue().toPlainString());
            return null;
        });
    }

    public void reject(long id, String reason, User actor) throws SQLException {
        String explanation = reason == null ? "" : reason.strip();
        if (explanation.isEmpty() || explanation.length() > 500) {
            throw new ServiceException("Nhập lý do từ chối (tối đa 500 ký tự).");
        }
        Db.inTransaction(conn -> {
            Adjustment adjustment = pending(conn, id, actor);
            dao.decide(conn, id, "REJECTED", explanation, actor.getId());
            auditDao.log(conn, actor.getId(), "STOCK_ADJUSTMENT_REJECT", "STOCK_ADJUSTMENT", id,
                    adjustment.code() + " · " + explanation);
            return null;
        });
    }

    /** Phiếu chờ duyệt, người duyệt là Quản lý kho phụ trách kho và không phải người lập. */
    private Adjustment pending(Connection conn, long id, User actor) throws SQLException {
        if (!canApprove(actor)) {
            throw new ForbiddenException("Chỉ Quản lý kho được duyệt phiếu điều chỉnh.");
        }
        Adjustment adjustment = dao.find(conn, id, true);
        Set<Long> scope = scope(conn, actor);
        if (adjustment == null || (scope != null && !scope.contains(adjustment.warehouseId()))) {
            throw new NotFoundException();
        }
        if (!"PENDING".equals(adjustment.status())) {
            throw new ServiceException("Phiếu " + adjustment.code() + " đã được xử lý.");
        }
        if (adjustment.createdBy() == actor.getId()) {
            throw new ServiceException("Người lập phiếu không được tự duyệt phiếu của mình.");
        }
        return adjustment;
    }

    private static boolean canApprove(User user) {
        return user.isAdmin() || user.hasRole(Role.WH_MANAGER);
    }

    /** null = mọi kho (Admin); nhân viên kho / quản lý kho chỉ thấy kho được gắn. */
    private Set<Long> scope(Connection conn, User user) throws SQLException {
        if (user.isAdmin()) {
            return null;
        }
        if (!user.hasRole(Role.WAREHOUSE) && !user.hasRole(Role.WH_MANAGER)) {
            throw new ForbiddenException("Chỉ nhân viên kho và quản lý kho được điều chỉnh tồn.");
        }
        return dao.userWarehouses(conn, user.getId());
    }
}
