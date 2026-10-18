package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.InventoryLotDao;
import com.oms.dao.LookupDao;
import com.oms.dao.PermissionDao;
import com.oms.dao.UserDao;
import com.oms.model.Module;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** S6-01: cấu hình, tra cứu lô và kiểm hạn dùng trong transaction xuất kho của team. */
public class InventoryLotService {
    public record Page(List<InventoryLotDao.Product> products, List<NamedItem> warehouses,
                       InventoryLotDao.Product selected, List<InventoryLotDao.Lot> lots,
                       boolean canManage, long soon, long expired, boolean truncated) {
        public List<InventoryLotDao.Product> getProducts() { return products; }
        public List<NamedItem> getWarehouses() { return warehouses; }
        public InventoryLotDao.Product getSelected() { return selected; }
        public List<InventoryLotDao.Lot> getLots() { return lots; }
        public boolean isCanManage() { return canManage; }
        public long getSoon() { return soon; }
        public long getExpired() { return expired; }
        public boolean isTruncated() { return truncated; }
    }

    private final InventoryLotDao dao = new InventoryLotDao();

    public Page page(Long product, Long warehouse, String status, String query, User viewer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            User user = permitted(conn, viewer, false);
            Set<Long> scope = scope(user);
            if (warehouse != null && scope != null && !scope.contains(warehouse)) throw new ForbiddenException("Kho nằm ngoài phạm vi được gắn.");
            List<InventoryLotDao.Product> products = dao.products(conn);
            InventoryLotDao.Product selected = product == null ? null : dao.product(conn, product, false);
            if (product != null && selected == null) throw new ServiceException("Sản phẩm không tồn tại.");
            String q = query == null ? "" : query.strip().toLowerCase(java.util.Locale.ROOT);
            List<InventoryLotDao.Lot> matching = dao.lots(conn, product, warehouse, false).stream()
                    .filter(l -> scope == null || scope.contains(l.warehouseId()))
                    .filter(l -> status == null || status.isBlank() || status.equals(l.getStatus()))
                    .filter(l -> q.isEmpty() || (l.sku()+" "+l.productName()+" "+l.number()).toLowerCase(java.util.Locale.ROOT).contains(q))
                    .toList();
            List<InventoryLotDao.Lot> lots = matching.stream().limit(200).toList();
            List<NamedItem> warehouses = new LookupDao().warehouses(conn).stream().filter(w -> scope == null || scope.contains(w.getId())).toList();
            return new Page(products,warehouses,selected,lots,canManage(conn,user),
                    lots.stream().filter(l -> "soon".equals(l.getStatus())).count(),
                    lots.stream().filter(l -> "expired".equals(l.getStatus())).count(),matching.size()>200);
        }
    }

    public void configure(long product, boolean tracked, int days, User actor) throws SQLException {
        if (days<0 || days>365) throw new ServiceException("Ngưỡng cảnh báo phải từ 0 đến 365 ngày.");
        Db.inTransaction(conn -> {
            User user = permitted(conn,actor,true);
            InventoryLotDao.Product before = dao.product(conn,product,true);
            if (before == null) throw new ServiceException("Sản phẩm không tồn tại.");
            // Không cho tắt rồi xuất để né kiểm hạn dùng khi còn hàng.
            if (before.tracked() != tracked) {
                try (var ps = conn.prepareStatement("SELECT COALESCE(SUM(on_hand),0),COALESCE(SUM(reserved),0) FROM warehouse_inventory WHERE product_id=?")) {
                    ps.setLong(1,product);
                    try (var rs = ps.executeQuery()) {
                        rs.next();
                        if (!tracked && (rs.getBigDecimal(1).signum()>0 || rs.getBigDecimal(2).signum()>0))
                            throw new ServiceException("Chỉ tắt quản lý lô khi SKU không còn tồn và giữ chỗ tại mọi kho.");
                    }
                }
                if (tracked) validateOpeningLots(conn,product);
            }
            dao.configure(conn,product,tracked,days);
            new AuditLogDao().log(conn,user.getId(),"LOT_CONFIG_UPDATE","PRODUCT",product,before.sku(),
                    before.tracked()+" / "+before.warningDays(),tracked+" / "+days);
            return null;
        });
    }

    public void updateMetadata(long lotId, String number, LocalDate manufactured, LocalDate expiry, User actor) throws SQLException {
        String lotNumber = number == null ? "" : number.strip();
        if (lotNumber.isEmpty() || lotNumber.length()>50 || manufactured==null || expiry==null || manufactured.isAfter(expiry) || manufactured.isAfter(LocalDate.now()))
            throw new ServiceException("Nhập số lô (tối đa 50 ký tự), ngày sản xuất không sau hôm nay và hạn dùng không trước ngày sản xuất.");
        Db.inTransaction(conn -> {
            User user = permitted(conn,actor,true);
            InventoryLotDao.Lot found = dao.find(conn,lotId,false);
            if (found==null) throw new ServiceException("Lô không tồn tại.");
            dao.product(conn,found.productId(),true);
            InventoryLotDao.Lot lot = dao.find(conn,lotId,true);
            if (scope(user)!=null && !scope(user).contains(lot.warehouseId())) throw new ForbiddenException("Lô không thuộc kho được gắn.");
            if (lot.tracked() && lot.number()!=null && !lot.number().isBlank() && lot.manufactured()!=null && lot.expiry()!=null)
                throw new ServiceException("Lô đã có đủ thông tin khi quản lý lô đang bật; không sửa hạn dùng để né kiểm tra quá hạn.");
            dao.metadata(conn,lotId,lotNumber,manufactured,expiry);
            new AuditLogDao().log(conn,user.getId(),"LOT_METADATA_UPDATE","INVENTORY_LOT",lotId,
                    lot.sku()+" / "+lotNumber,String.valueOf(lot.number())+" / "+lot.manufactured()+" / "+lot.expiry(),lotNumber+" / "+manufactured+" / "+expiry);
            return null;
        });
    }

    /** Lô gợi ý xuất (GHTK-61) cho một SKU có quản lý lô: lô còn hạn, hạn gần nhất trước. */
    public record LotChoice(long productId, String sku, String productName, BigDecimal quantity,
                            List<InventoryLotDao.Lot> options, String suggestion) {
        public long getProductId() { return productId; }
        public String getSku() { return sku; }
        public String getProductName() { return productName; }
        public BigDecimal getQuantity() { return quantity; }
        public List<InventoryLotDao.Lot> getOptions() { return options; }
        public String getSuggestion() { return suggestion; }
        public long getSuggestedLotId() { return options.isEmpty() ? 0 : options.get(0).id(); }
    }

    /** Gợi ý FEFO cho màn xuất kho; chỉ SKU có quản lý lô. Không khoá, chỉ để hiển thị. */
    public List<LotChoice> choices(Connection conn, long warehouse, Map<Long,BigDecimal> quantities) throws SQLException {
        List<LotChoice> list = new java.util.ArrayList<>();
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            if (entry.getValue().signum()<=0) continue;
            InventoryLotDao.Product product = dao.product(conn,entry.getKey(),false);
            if (!product.tracked()) continue;
            var usable = usable(product,dao.lots(conn,entry.getKey(),warehouse,false),false);
            StringBuilder suggestion = new StringBuilder();
            BigDecimal remaining = entry.getValue();
            for (var lot : usable) {
                if (remaining.signum()==0) break;
                BigDecimal taken = remaining.min(lot.remaining());
                if (suggestion.length()>0) suggestion.append("; ");
                suggestion.append(lot.number()).append(" (HSD ").append(lot.expiry()).append(") × ").append(taken.stripTrailingZeros().toPlainString());
                remaining = remaining.subtract(taken);
            }
            if (remaining.signum()>0) suggestion.append(suggestion.length()>0?"; ":"").append("thiếu ").append(remaining.stripTrailingZeros().toPlainString());
            list.add(new LotChoice(product.id(),product.sku(),product.name(),entry.getValue(),usable,suggestion.toString()));
        }
        return list;
    }

    /** Lô dùng được theo thứ tự xuất: còn hạn trước, hạn gần nhất trước; lô quá hạn chỉ khi có ghi đè. */
    private static List<InventoryLotDao.Lot> usable(InventoryLotDao.Product product, List<InventoryLotDao.Lot> lots, boolean override) {
        return lots.stream().filter(l -> l.remaining().signum()>0)
                .filter(l -> !product.tracked() || !l.expiry().isBefore(LocalDate.now()) || override)
                .sorted(java.util.Comparator.comparing((InventoryLotDao.Lot l) -> product.tracked() && l.expiry().isBefore(LocalDate.now()))
                        .thenComparing(l -> l.expiry()==null?LocalDate.MAX:l.expiry()).thenComparingLong(InventoryLotDao.Lot::id)).toList();
    }

    public void consume(Connection conn, long shipment, long warehouse, Map<Long,BigDecimal> quantities,
                        boolean override, String reason, User actor) throws SQLException {
        consume(conn,shipment,warehouse,quantities,override,reason,Map.of(),null,actor);
    }

    /**
     * Gọi sau khi khoá số tồn và lập phiếu xuất; lỗi rollback cả phiếu, giữ chỗ và lô.
     * chosen: SKU → lô thủ kho chọn thay lô gợi ý; chọn khác lô gợi ý thì bắt buộc pickReason.
     */
    public void consume(Connection conn, long shipment, long warehouse, Map<Long,BigDecimal> quantities,
                        boolean override, String reason, Map<Long,Long> chosen, String pickReason, User actor) throws SQLException {
        String explanation = reason == null ? "" : reason.strip();
        String pickExplanation = pickReason == null ? "" : pickReason.strip();
        User verified = override ? permitted(conn,actor,true) : actor;
        if (override && (!verified.hasRole(Role.WH_MANAGER) || explanation.isEmpty() || explanation.length()>500
                || (scope(verified)!=null && !scope(verified).contains(warehouse))))
            throw new ForbiddenException("Chỉ Quản lý kho được ghi đè hạn dùng, phải xác nhận và nhập lý do tối đa 500 ký tự.");
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            if (entry.getValue().signum()<=0) continue;
            InventoryLotDao.Product product = dao.product(conn,entry.getKey(),true);
            List<InventoryLotDao.Lot> lots = dao.lots(conn,entry.getKey(),warehouse,true);
            if (product.tracked() && lots.stream().anyMatch(l -> l.remaining().signum()>0 && "incomplete".equals(l.getStatus())))
                throw new ServiceException("SKU "+product.sku()+" còn lô thiếu số lô, ngày sản xuất hoặc hạn dùng. Bổ sung trước khi xuất.");
            var usable = new java.util.ArrayList<>(usable(product,lots,override));
            Long picked = product.tracked() ? chosen.get(entry.getKey()) : null;
            boolean manual = picked != null && !usable.isEmpty() && usable.get(0).id() != picked;
            if (manual) {
                var lot = usable.stream().filter(l -> l.id()==picked).findFirst()
                        .orElseThrow(() -> new ServiceException("Lô đã chọn cho SKU "+product.sku()+" không còn xuất được."));
                if (pickExplanation.isEmpty() || pickExplanation.length()>500)
                    throw new ServiceException("Chọn lô khác lô gợi ý cho SKU "+product.sku()+" phải nhập lý do (tối đa 500 ký tự).");
                usable.remove(lot);
                usable.add(0,lot);
            }
            BigDecimal remaining = entry.getValue();
            for (var lot : usable) {
                if (remaining.signum()==0) break;
                BigDecimal taken = remaining.min(lot.remaining());
                boolean expired = product.tracked() && lot.expiry().isBefore(LocalDate.now());
                boolean chosenLot = manual && lot.id()==picked;
                String note = expired ? explanation : chosenLot ? pickExplanation : null;
                dao.allocate(conn,shipment,lot,taken,note,note==null?null:actor.getId());
                if (expired) new AuditLogDao().log(conn,actor.getId(),"LOT_EXPIRY_OVERRIDE","SHIPMENT",shipment,
                        lot.number()+" / "+taken.toPlainString()+" / "+explanation);
                if (chosenLot) new AuditLogDao().log(conn,actor.getId(),"LOT_MANUAL_PICK","SHIPMENT",shipment,
                        lot.number()+" thay "+usable.get(1).number()+" / "+taken.toPlainString()+" / "+pickExplanation);
                remaining=remaining.subtract(taken);
            }
            if (product.tracked() && remaining.signum()>0)
                throw new ServiceException("SKU "+product.sku()+" không đủ lượng lô còn hạn. Kiểm tra lại lô; ghi đè quá hạn chỉ dành cho Quản lý kho có lý do.");
        }
    }

    private void validateOpeningLots(Connection conn, long product) throws SQLException {
        List<InventoryLotDao.Lot> lots = dao.lots(conn,product,null,true);
        if (lots.stream().anyMatch(l -> l.remaining().signum()>0 && "incomplete".equals(l.getStatus())))
            throw new ServiceException("Bổ sung thông tin các lô cũ trước khi bật quản lý lô.");
        Map<Long,BigDecimal> totals = lots.stream().collect(Collectors.toMap(InventoryLotDao.Lot::warehouseId,InventoryLotDao.Lot::remaining,BigDecimal::add));
        try (var ps = conn.prepareStatement("SELECT warehouse_id,on_hand FROM warehouse_inventory WHERE product_id=? ORDER BY warehouse_id FOR UPDATE")) {
            ps.setLong(1,product);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal quantity = totals.remove(rs.getLong(1));
                    if ((quantity==null?BigDecimal.ZERO:quantity).compareTo(rs.getBigDecimal(2))!=0)
                        throw new ServiceException("Lượng lô cũ chưa khớp sổ tồn; cần đối chiếu tồn thực tế trước khi bật quản lý lô.");
                }
            }
        }
        if (totals.values().stream().anyMatch(q -> q.signum()!=0)) throw new ServiceException("Có lượng lô chưa có trong sổ tồn; cần đối chiếu trước khi bật quản lý lô.");
    }

    private static User permitted(Connection conn, User actor, boolean write) throws SQLException {
        if (actor==null) throw new ForbiddenException("Cần đăng nhập.");
        User fresh = new UserDao().findById(conn,actor.getId());
        if (fresh==null || fresh.getStatus()!=User.Status.ACTIVE || fresh.getSessionVersion()!=actor.getSessionVersion()) throw new ForbiddenException("Phiên hoặc tài khoản không còn hiệu lực.");
        PermissionPolicy policy = new PermissionPolicy(new PermissionDao().loadMatrix(conn));
        if (!policy.canRead(fresh.getRoles(),Module.INVENTORY) || (write && !canManage(conn,fresh)))
            throw new ForbiddenException("Bạn không có quyền quản lý lô.");
        return fresh;
    }

    private static boolean canManage(Connection conn, User user) throws SQLException {
        return (user.isAdmin() || user.hasRole(Role.WH_MANAGER)) && new PermissionPolicy(new PermissionDao().loadMatrix(conn)).canWrite(user.getRoles(),Module.INVENTORY);
    }

    private static Set<Long> scope(User user) {
        return user.isAdmin() || (!user.hasRole(Role.WAREHOUSE) && !user.hasRole(Role.WH_MANAGER)) ? null : user.getWarehouses().stream().map(NamedItem::getId).collect(Collectors.toSet());
    }
}
