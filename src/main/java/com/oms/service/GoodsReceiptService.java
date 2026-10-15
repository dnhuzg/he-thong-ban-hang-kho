package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.GoodsReceiptDao;
import com.oms.dao.LookupDao;
import com.oms.model.GoodsReceipt;
import com.oms.model.NamedItem;
import com.oms.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phiếu nhập kho từ nhà cung cấp (S5-04). Phiếu nháp không ảnh hưởng tồn; xác nhận phiếu mới cộng tồn,
 * tạo lô và ghi bút toán trong cùng transaction. Phiếu đã xác nhận không sửa được.
 */
public class GoodsReceiptService {

    private static final int MAX_LINES = 200;
    private static final int RECENT = 30;
    private static final BigDecimal MAX_BASE_QUANTITY = new BigDecimal("1000000000");

    public record LineInput(Long productId, Long unitId, BigDecimal quantity, String lotNo, LocalDate expiryDate, LocalDate manufacturedDate) {
        public LineInput(Long productId, Long unitId, BigDecimal quantity, String lotNo, LocalDate expiryDate) {
            this(productId,unitId,quantity,lotNo,expiryDate,null);
        }
    }

    public record Form(Long id, Long supplierId, String documentNo, LocalDate receivedDate, Long warehouseId,
                       String note, List<LineInput> lines, boolean confirm) {
    }

    /** Dữ liệu cho trang phiếu nhập: danh mục để chọn, phiếu gần đây và phiếu đang mở (nếu có). */
    public record PageData(List<GoodsReceiptDao.Option> suppliers, List<GoodsReceiptDao.Option> warehouses,
                           List<GoodsReceiptDao.ProductOption> products, List<GoodsReceipt.Summary> recent,
                           GoodsReceipt current) {
    }

    public record Saved(long id, String code, String status) {
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy phiếu nhập.");
        }
    }

    private final GoodsReceiptDao dao = new GoodsReceiptDao();
    private final LookupDao lookupDao = new LookupDao();
    private final UnitConversionService conversions = new UnitConversionService();
    private final AuditLogDao auditDao = new AuditLogDao();

    public PageData page(Long receiptId, User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Set<Long> scope = warehouseScope(viewer);
            List<GoodsReceiptDao.Option> warehouses = new ArrayList<>();
            for (NamedItem w : lookupDao.warehouses(conn)) {
                if (scope == null || scope.contains(w.getId())) {
                    warehouses.add(new GoodsReceiptDao.Option(w.getId(), w.getName()));
                }
            }
            GoodsReceipt current = null;
            if (receiptId != null) {
                current = dao.find(conn, receiptId);
                if (current == null || !inScope(scope, current.warehouseId())) {
                    throw new NotFoundException();
                }
            }
            return new PageData(dao.activeSuppliers(conn), warehouses, dao.activeProducts(conn),
                    dao.recent(conn, scope, RECENT), current);
        }
    }

    public Saved save(Form form, User actor) throws SQLException {
        String documentNo = trim(form.documentNo());
        String note = trim(form.note());
        if (form.supplierId() == null) {
            throw new ServiceException("Chọn nhà cung cấp.");
        }
        if (documentNo == null) {
            throw new ServiceException("Nhập số chứng từ.");
        }
        if (documentNo.length() > 50) {
            throw new ServiceException("Số chứng từ tối đa 50 ký tự.");
        }
        if (form.receivedDate() == null) {
            throw new ServiceException("Chọn ngày nhập.");
        }
        if (form.receivedDate().isAfter(LocalDate.now())) {
            throw new ServiceException("Ngày nhập không được sau hôm nay.");
        }
        if (form.warehouseId() == null) {
            throw new ServiceException("Chọn kho nhập.");
        }
        if (note != null && note.length() > 500) {
            throw new ServiceException("Ghi chú tối đa 500 ký tự.");
        }
        if (form.lines() == null || form.lines().isEmpty()) {
            throw new ServiceException("Phiếu cần ít nhất một mặt hàng.");
        }
        if (form.lines().size() > MAX_LINES) {
            throw new ServiceException("Một phiếu tối đa " + MAX_LINES + " mặt hàng.");
        }
        Set<Long> scope = warehouseScope(actor);
        if (!inScope(scope, form.warehouseId())) {
            throw new ForbiddenException("Bạn không được nhập hàng vào kho này.");
        }

        return Db.inTransaction(conn -> {
            if (!dao.isActiveSupplier(conn, form.supplierId())) {
                throw new ServiceException("Nhà cung cấp không tồn tại hoặc đã ngừng giao dịch.");
            }
            if (!lookupDao.warehouses(conn).stream().anyMatch(w -> w.getId() == form.warehouseId())) {
                throw new ServiceException("Kho nhập không tồn tại.");
            }
            List<GoodsReceipt.Line> lines = new ArrayList<>();
            for (long productId : form.lines().stream().map(LineInput::productId).filter(java.util.Objects::nonNull).distinct().sorted().toList())
                new com.oms.dao.InventoryLotDao().product(conn,productId,true);
            int no = 1;
            for (LineInput input : form.lines()) {
                lines.add(line(conn, input, form.receivedDate(), no++));
            }

            long id;
            if (form.id() == null) {
                if (dao.documentTaken(conn, form.supplierId(), documentNo, 0)) {
                    throw new ServiceException("Số chứng từ này đã có phiếu của nhà cung cấp đã chọn.");
                }
                id = dao.insert(conn, form.supplierId(), documentNo, form.receivedDate(), form.warehouseId(), note,
                        actor.getId());
            } else {
                GoodsReceiptDao.Header header = dao.findHeaderForUpdate(conn, form.id());
                if (header == null || !inScope(scope, header.warehouseId())) {
                    throw new NotFoundException();
                }
                if (!GoodsReceipt.DRAFT.equals(header.status())) {
                    throw new ServiceException("Phiếu đã xác nhận không sửa được; cần lập phiếu điều chỉnh.");
                }
                if (dao.documentTaken(conn, form.supplierId(), documentNo, form.id())) {
                    throw new ServiceException("Số chứng từ này đã có phiếu của nhà cung cấp đã chọn.");
                }
                id = form.id();
                dao.updateHeader(conn, id, form.supplierId(), documentNo, form.receivedDate(), form.warehouseId(),
                        note);
            }
            dao.replaceLines(conn, id, lines);

            if (form.confirm()) {
                dao.markConfirmed(conn, id, actor.getId());
                for (GoodsReceipt.Line line : lines) {
                    dao.addOnHand(conn, form.warehouseId(), line.productId(), line.baseQuantity());
                    dao.insertLot(conn, form.warehouseId(), line.productId(), line.baseQuantity(),
                            form.receivedDate(), id, line.lotNo(), line.manufacturedDate(), line.expiryDate());
                    dao.insertMovement(conn, form.warehouseId(), line.productId(), line.baseQuantity(), id,
                            actor.getId());
                }
            }
            GoodsReceipt saved = dao.find(conn, id);
            auditDao.log(conn, actor.getId(), form.confirm() ? "GOODS_RECEIPT_CONFIRM" : "GOODS_RECEIPT_SAVE",
                    "GOODS_RECEIPT", id, saved.code() + " · chứng từ " + documentNo + " · " + lines.size()
                            + " mặt hàng · " + saved.warehouseName());
            return new Saved(id, saved.code(), saved.status());
        });
    }

    private GoodsReceipt.Line line(Connection conn, LineInput input, LocalDate receivedDate, int no)
            throws SQLException {
        String at = "Dòng " + no + ": ";
        if (input.productId() == null || input.unitId() == null || input.quantity() == null) {
            throw new ServiceException(at + "chọn sản phẩm, đơn vị và số lượng.");
        }
        if (!dao.isActiveProduct(conn, input.productId())) {
            throw new ServiceException(at + "sản phẩm không tồn tại hoặc đã ngừng kinh doanh.");
        }
        var config = new com.oms.dao.InventoryLotDao().product(conn,input.productId(),true);
        String lotNo = trim(input.lotNo());
        if (lotNo != null && lotNo.length() > 50) {
            throw new ServiceException(at + "số lô tối đa 50 ký tự.");
        }
        if (input.expiryDate() != null && input.expiryDate().isBefore(receivedDate)) {
            throw new ServiceException(at + "hạn dùng không được trước ngày nhập.");
        }
        if (config.tracked() && (lotNo==null || input.manufacturedDate()==null || input.expiryDate()==null))
            throw new ServiceException(at+"SKU quản lý lô phải có số lô, ngày sản xuất và hạn dùng.");
        if (input.manufacturedDate()!=null && (input.manufacturedDate().isAfter(receivedDate)
                || (input.expiryDate()!=null && input.manufacturedDate().isAfter(input.expiryDate()))))
            throw new ServiceException(at+"Ngày sản xuất không được sau ngày nhập hoặc hạn dùng.");
        if (input.quantity().stripTrailingZeros().scale() > 4) {
            throw new ServiceException(at + "số lượng tối đa 4 chữ số thập phân.");
        }
        UnitConversionService.Converted converted;
        try {
            converted = conversions.toBase(conn, input.productId(), input.unitId(), input.quantity());
        } catch (ServiceException e) {
            throw new ServiceException(at + e.getMessage());
        }
        if (converted.baseQuantity().signum() <= 0 || converted.baseQuantity().compareTo(MAX_BASE_QUANTITY) > 0) {
            throw new ServiceException(at + "số lượng quy đổi không hợp lệ.");
        }
        return new GoodsReceipt.Line(input.productId(), input.unitId(), input.quantity(), converted.factor(),
                converted.baseQuantity(), lotNo, input.expiryDate(), input.manufacturedDate());
    }

    /** null = mọi kho (Admin); người khác chỉ thao tác trên kho được gắn. */
    private static Set<Long> warehouseScope(User user) {
        if (user.isAdmin()) {
            return null;
        }
        return user.getWarehouses().stream().map(NamedItem::getId).collect(Collectors.toSet());
    }

    private static boolean inScope(Set<Long> scope, long warehouseId) {
        return scope == null || scope.contains(warehouseId);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
