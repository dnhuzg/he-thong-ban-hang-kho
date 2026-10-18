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
import com.oms.dao.PaymentDao;
import com.oms.model.Payment;
import com.oms.model.Role;
import com.oms.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/** In phiếu thu (S7-09): mỗi khoản đã ghi nhận có mã PT duy nhất, xem trước và in / tải PDF. */
public class PaymentReceiptService {

    private static final int LIST_LIMIT = 100;

    private final PaymentDao dao = new PaymentDao();

    public static class NotFoundException extends RuntimeException {
    }

    public List<Payment> list(User viewer) throws SQLException {
        requireAccountant(viewer);
        try (Connection conn = Db.getConnection()) {
            return dao.recent(conn, LIST_LIMIT);
        }
    }

    public Payment get(long id, User viewer) throws SQLException {
        requireAccountant(viewer);
        try (Connection conn = Db.getConnection()) {
            Payment payment = dao.find(conn, id);
            if (payment == null) {
                throw new NotFoundException();
            }
            return payment;
        }
    }

    public byte[] pdf(long id, User viewer) throws SQLException, IOException {
        Payment payment = get(id, viewer);
        Font normal = new Font(BaseFont.createFont("liberation/LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 11);
        Font bold = new Font(BaseFont.createFont("liberation/LiberationSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED), 11);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A5.rotate(), 36, 36, 30, 30);
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            Paragraph company = new Paragraph("HỆ THỐNG BÁN HÀNG & KHO", bold);
            company.setAlignment(Element.ALIGN_CENTER);
            document.add(company);
            Paragraph title = new Paragraph("PHIẾU THU", new Font(bold.getBaseFont(), 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            Paragraph code = new Paragraph("Số: " + payment.getCode() + "   ·   Ngày thu: " + payment.getPaidOnText(), normal);
            code.setAlignment(Element.ALIGN_CENTER);
            code.setSpacingAfter(12);
            document.add(code);

            PdfPTable details = new PdfPTable(new float[] {35, 65});
            details.setWidthPercentage(100);
            String[][] rows = {
                    {"Đại lý nộp tiền", payment.getCustomerCode() + " – " + payment.getCustomerName()},
                    {"Số tiền", money(payment.getAmount()) + " ₫"},
                    {"Hình thức", payment.getMethod().getLabel()},
                    {"Người thu", payment.getCollectedByName()},
                    {"Đối trừ hoá đơn", payment.getInvoiceCodes() == null ? "" : payment.getInvoiceCodes()},
                    {"Ghi chú", payment.getNote() == null ? "" : payment.getNote()},
            };
            for (String[] row : rows) {
                PdfPCell label = new PdfPCell(new Phrase(row[0], bold));
                label.setPadding(6);
                details.addCell(label);
                PdfPCell value = new PdfPCell(new Phrase(row[1], normal));
                value.setPadding(6);
                details.addCell(value);
            }
            document.add(details);

            PdfPTable signatures = new PdfPTable(2);
            signatures.setWidthPercentage(100);
            signatures.setSpacingBefore(18);
            for (String who : new String[] {"Người nộp tiền", "Người thu tiền"}) {
                PdfPCell cell = new PdfPCell(new Phrase(who + "\n(Ký, ghi rõ họ tên)\n\n\n", bold));
                cell.setBorder(PdfPCell.NO_BORDER);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                signatures.addCell(cell);
            }
            document.add(signatures);
            document.close();
            return output.toByteArray();
        } catch (DocumentException e) {
            throw new IOException("Không tạo được PDF phiếu thu", e);
        }
    }

    private static void requireAccountant(User user) {
        if (!user.isAdmin() && !user.hasRole(Role.ACCOUNTANT)) {
            throw new ForbiddenException("Chỉ kế toán công nợ được xem và in phiếu thu.");
        }
    }

    private static String money(BigDecimal value) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }
}
