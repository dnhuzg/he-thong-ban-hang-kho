package com.oms.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.CustomerDao;
import com.oms.dao.InvoiceDao;
import com.oms.dao.OrderCreditDao;
import com.oms.dao.PortalAccountDao;
import com.oms.model.Customer;
import com.oms.model.Invoice;
import com.oms.model.Role;
import com.oms.model.User;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Phát hành hoá đơn (S7-01) từ phiếu xuất đã giao: tính theo số lượng thực giao và giá đã chốt trên đơn,
 * gộp nhiều phiếu của cùng đại lý, số hoá đơn liên tục. Phát hành ghi khoản phải thu (công nợ thật)
 * và nhả phần hạn mức đang cam kết của đơn trong cùng transaction. Hoá đơn đã phát hành không sửa, không xoá.
 */
public class InvoiceService {

    private static final int MAX_SHIPMENTS = 50;
    private static final int RECENT = 30;

    public record Page(InvoiceDao.Summary summary, List<Invoice.Eligible> eligible, List<Invoice> recent,
                       List<InvoiceDao.Debtor> debtors) {
        public InvoiceDao.Summary getSummary() { return summary; }
        public List<Invoice.Eligible> getEligible() { return eligible; }
        public List<Invoice> getRecent() { return recent; }
        public List<InvoiceDao.Debtor> getDebtors() { return debtors; }
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy hoá đơn.");
        }
    }

    private final InvoiceDao dao = new InvoiceDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public Page page(User viewer) throws SQLException {
        requireAccountant(viewer);
        try (Connection conn = Db.getConnection()) {
            LocalDate today = LocalDate.now();
            return new Page(dao.summary(conn, today), dao.eligible(conn), dao.recent(conn, RECENT), dao.debtors(conn, today));
        }
    }

    public Invoice get(long id, User viewer) throws SQLException {
        requireAccountant(viewer);
        try (Connection conn = Db.getConnection()) {
            Invoice invoice = dao.find(conn, id);
            if (invoice == null) {
                throw new NotFoundException();
            }
            return invoice;
        }
    }

    /** Trả về ID hoá đơn vừa phát hành. */
    public long issue(List<Long> shipmentIds, User actor) throws SQLException {
        requireAccountant(actor);
        // Khoá phiếu theo thứ tự ID để hai người lập cùng lúc không bị deadlock
        TreeSet<Long> ids = new TreeSet<>(shipmentIds);
        if (ids.isEmpty()) {
            throw new ServiceException("Chọn ít nhất một phiếu xuất đã giao để lập hoá đơn.");
        }
        if (ids.size() > MAX_SHIPMENTS) {
            throw new ServiceException("Một hoá đơn gộp tối đa " + MAX_SHIPMENTS + " phiếu xuất.");
        }
        return Db.inTransaction(conn -> {
            List<InvoiceDao.ShipmentLock> shipments = new ArrayList<>();
            Long customerId = null;
            for (long id : ids) {
                InvoiceDao.ShipmentLock shipment = dao.lockShipment(conn, id);
                if (shipment == null || shipment.orderId() == null) {
                    throw new ServiceException("Không tìm thấy phiếu xuất #" + id + ".");
                }
                if (!"DELIVERED".equals(shipment.status())) {
                    throw new ServiceException(String.format("Phiếu PX%06d chưa giao thành công nên chưa lập hoá đơn được.", id));
                }
                if (shipment.invoiceId() != null) {
                    throw new ServiceException(String.format("Phiếu PX%06d đã được lập hoá đơn.", id));
                }
                if (customerId != null && customerId != shipment.customerId()) {
                    throw new ServiceException("Chỉ gộp được các phiếu xuất của cùng một đại lý vào một hoá đơn.");
                }
                customerId = shipment.customerId();
                shipments.add(shipment);
            }
            Customer customer = new CustomerDao().findByIdForUpdate(conn, customerId);

            record Priced(long shipmentId, long productId, BigDecimal quantity, BigDecimal unitPrice, BigDecimal amount) {
            }
            List<Priced> lines = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            Set<Long> orderIds = new LinkedHashSet<>();
            for (InvoiceDao.ShipmentLock shipment : shipments) {
                orderIds.add(shipment.orderId());
                for (InvoiceDao.Delivered item : dao.delivered(conn, shipment.id(), shipment.orderId())) {
                    if (item.quantity() == null || item.orderNet() == null || item.orderQuantity() == null
                            || item.orderQuantity().signum() <= 0) {
                        throw new ServiceException(String.format(
                                "Phiếu PX%06d có mặt hàng không tìm được giá đã chốt trên đơn.", shipment.id()));
                    }
                    // Tính tiền theo tỉ lệ thực giao trên tổng tiền đã chốt để không lệch do làm tròn đơn giá
                    BigDecimal amount = item.orderNet().multiply(item.quantity())
                            .divide(item.orderQuantity(), 0, RoundingMode.HALF_UP);
                    BigDecimal unitPrice = item.orderNet().divide(item.orderQuantity(), 2, RoundingMode.HALF_UP);
                    lines.add(new Priced(shipment.id(), item.productId(), item.quantity(), unitPrice, amount));
                    total = total.add(amount);
                }
            }
            if (lines.isEmpty()) {
                throw new ServiceException("Các phiếu xuất đã chọn không có dòng hàng.");
            }

            String code = String.format("HD%06d", dao.nextNumber(conn, "INVOICE"));
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = now.toLocalDate();
            long invoiceId = dao.insert(conn, code, customerId, dao.portalUser(conn, customerId), total, now,
                    today.plusDays(Math.max(0, customer.getCreditDays())), actor.getId());
            for (Priced line : lines) {
                dao.insertLine(conn, invoiceId, line.shipmentId(), line.productId(), line.quantity(), line.unitPrice(),
                        line.amount());
            }
            for (InvoiceDao.ShipmentLock shipment : shipments) {
                dao.linkShipment(conn, shipment.id(), invoiceId);
            }
            dao.insertReceivable(conn, customerId, code, today, total);
            // Phần đơn đã thành công nợ thật nên không còn giữ chỗ hạn mức
            OrderCreditDao credit = new OrderCreditDao();
            for (long orderId : orderIds) {
                credit.release(conn, orderId);
            }
            auditDao.log(conn, actor.getId(), "INVOICE_ISSUE", "INVOICE", invoiceId,
                    code + " · " + customer.getCode() + " · " + shipments.size() + " phiếu xuất · " + money(total) + " ₫");
            return invoiceId;
        });
    }

    public byte[] pdf(long id, User viewer) throws SQLException, IOException {
        return render(get(id, viewer));
    }

    /** Đại lý tải lại hoá đơn của chính mình trên cổng; hoá đơn của đại lý khác coi như không tồn tại. */
    public Invoice getForCustomer(long id, User customer) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Long own = new PortalAccountDao().customerId(conn, customer.getId());
            Invoice invoice = dao.find(conn, id);
            if (own == null || invoice == null || invoice.getCustomerId() != own || invoice.getStatus() == Invoice.Status.CANCELLED) {
                throw new NotFoundException();
            }
            return invoice;
        }
    }

    public byte[] pdfForCustomer(long id, User customer) throws SQLException, IOException {
        return render(getForCustomer(id, customer));
    }

    private byte[] render(Invoice invoice) throws IOException {
        Font normal = new Font(BaseFont.createFont("liberation/LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 10);
        Font bold = new Font(BaseFont.createFont("liberation/LiberationSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 10);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 42);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("HOÁ ĐƠN BÁN HÀNG " + invoice.getCode(), new Font(bold.getBaseFont(), 18)));
            document.add(new Paragraph("Đại lý: " + invoice.getCustomerCode() + " – " + invoice.getCustomerName(), normal));
            document.add(new Paragraph("Ngày phát hành: " + invoice.getIssuedAtText(), normal));
            document.add(new Paragraph("Hạn thanh toán: " + invoice.getDueDateText(), normal));
            document.add(new Paragraph("Người lập: " + (invoice.getIssuedByName() == null ? "" : invoice.getIssuedByName()), normal));
            document.add(new Paragraph(" ", normal));
            PdfPTable table = new PdfPTable(new float[] {60, 55, 150, 50, 75, 85});
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            for (String header : new String[] {"Phiếu xuất", "SKU", "Sản phẩm / ĐVT", "SL giao", "Đơn giá", "Thành tiền"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, bold));
                cell.setBackgroundColor(new Color(235, 239, 244));
                cell.setPadding(6);
                table.addCell(cell);
            }
            for (Invoice.Line line : invoice.getLines()) {
                String[] cells = {line.getShipmentCode(), line.getSku(), line.getProductName() + " / " + line.getBaseUnit(),
                        line.getQuantity().toPlainString(), money(line.getUnitPrice()), money(line.getAmount())};
                for (int i = 0; i < cells.length; i++) {
                    PdfPCell cell = new PdfPCell(new Phrase(cells[i], normal));
                    cell.setPadding(5);
                    if (i >= 3) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.add(new Paragraph("TỔNG TIỀN: " + money(invoice.getTotalAmount()) + " ₫", bold));
            document.close();
            return output.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Không tạo được PDF hoá đơn", e);
        }
    }

    private static void requireAccountant(User user) {
        if (!user.isAdmin() && !user.hasRole(Role.ACCOUNTANT)) {
            throw new ForbiddenException("Chỉ kế toán công nợ được lập và xem hoá đơn.");
        }
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }
}
