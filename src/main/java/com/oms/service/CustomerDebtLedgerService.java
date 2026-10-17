package com.oms.service;

import com.oms.config.Db;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Sổ chi tiết công nợ đại lý, lấy dữ liệu từ hóa đơn đã phát hành. */
public class CustomerDebtLedgerService {
    public record CustomerOption(long id, String code, String name) {}
    public record Entry(LocalDate date, String reference, String type, String note,
                        BigDecimal increase, BigDecimal decrease, BigDecimal balance) {}
    public record Report(long customerId, String customerCode, String customerName,
                         LocalDate from, LocalDate to, BigDecimal opening,
                         BigDecimal increase, BigDecimal decrease, BigDecimal closing,
                         List<Entry> entries) {}

    public List<CustomerOption> customers() throws SQLException {
        String sql = "SELECT id, code, name FROM customers ORDER BY name, code";
        List<CustomerOption> result = new ArrayList<>();
        try (Connection c = Db.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(new CustomerOption(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
        }
        return result;
    }

    public Report report(long customerId, LocalDate from, LocalDate to) throws SQLException {
        if (customerId <= 0) throw new IllegalArgumentException("Vui lòng chọn đại lý.");
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("Khoảng thời gian không hợp lệ.");
        try (Connection c = Db.getConnection()) {
            String customerSql = "SELECT code,name FROM customers WHERE id=?";
            String code, name;
            try (PreparedStatement ps = c.prepareStatement(customerSql)) {
                ps.setLong(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy đại lý.");
                    code = rs.getString("code"); name = rs.getString("name");
                }
            }
            List<RawEntry> all = new ArrayList<>();
            String sql = "SELECT code,total_amount,DATE(issued_at) issued_on,DATE(paid_at) paid_on,status " +
                    "FROM invoices WHERE customer_id=? AND status <> 'CANCELLED' ORDER BY issued_at,id";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amount = rs.getBigDecimal("total_amount");
                        LocalDate issued = rs.getDate("issued_on").toLocalDate();
                        String ref = rs.getString("code");
                        all.add(new RawEntry(issued, ref, "Hóa đơn", "Phát hành hóa đơn " + ref, amount, BigDecimal.ZERO));
                        Date paidDate = rs.getDate("paid_on");
                        if ("PAID".equals(rs.getString("status")) && paidDate != null) {
                            all.add(new RawEntry(paidDate.toLocalDate(), ref, "Thanh toán hóa đơn", "Thanh toán hóa đơn " + ref,
                                    BigDecimal.ZERO, amount));
                        }
                    }
                }
            }
            all.sort(java.util.Comparator.comparing(RawEntry::date).thenComparing(RawEntry::reference).thenComparing(RawEntry::type));
            BigDecimal opening = BigDecimal.ZERO, increase = BigDecimal.ZERO, decrease = BigDecimal.ZERO;
            List<Entry> entries = new ArrayList<>();
            BigDecimal balance = BigDecimal.ZERO;
            for (RawEntry e : all) {
                if (e.date().isBefore(from)) opening = opening.add(e.increase()).subtract(e.decrease());
            }
            balance = opening;
            for (RawEntry e : all) {
                if (e.date().isBefore(from) || e.date().isAfter(to)) continue;
                balance = balance.add(e.increase()).subtract(e.decrease());
                increase = increase.add(e.increase()); decrease = decrease.add(e.decrease());
                entries.add(new Entry(e.date(), e.reference(), e.type(), e.note(), e.increase(), e.decrease(), balance));
            }
            return new Report(customerId, code, name, from, to, opening, increase, decrease,
                    opening.add(increase).subtract(decrease), List.copyOf(entries));
        }
    }

    public byte[] exportXlsx(Report report) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sổ công nợ");
            CellStyle title = wb.createCellStyle(); org.apache.poi.ss.usermodel.Font bold = wb.createFont(); bold.setBold(true); title.setFont(bold);
            CellStyle money = wb.createCellStyle(); money.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            sheet.createRow(0).createCell(0).setCellValue("SỔ CHI TIẾT CÔNG NỢ ĐẠI LÝ"); sheet.getRow(0).getCell(0).setCellStyle(title);
            sheet.createRow(1).createCell(0).setCellValue("Đại lý: " + report.customerCode() + " - " + report.customerName());
            sheet.createRow(2).createCell(0).setCellValue("Kỳ: " + report.from() + " đến " + report.to());
            sheet.createRow(3).createCell(0).setCellValue("Dư đầu kỳ"); sheet.getRow(3).createCell(1).setCellValue(report.opening().doubleValue()); sheet.getRow(3).getCell(1).setCellStyle(money);
            String[] headers = {"Ngày", "Chứng từ", "Diễn giải", "Phát sinh tăng", "Phát sinh giảm", "Số dư lũy kế"};
            org.apache.poi.ss.usermodel.Row hr = sheet.createRow(5); for (int i=0;i<headers.length;i++){hr.createCell(i).setCellValue(headers[i]);hr.getCell(i).setCellStyle(title);}
            int r=6;
            for (Entry e: report.entries()) {
                org.apache.poi.ss.usermodel.Row row=sheet.createRow(r++); row.createCell(0).setCellValue(e.date().toString()); row.createCell(1).setCellValue(e.reference()); row.createCell(2).setCellValue(e.type()+" - "+e.note());
                row.createCell(3).setCellValue(e.increase().doubleValue()); row.getCell(3).setCellStyle(money);
                row.createCell(4).setCellValue(e.decrease().doubleValue()); row.getCell(4).setCellStyle(money);
                row.createCell(5).setCellValue(e.balance().doubleValue()); row.getCell(5).setCellStyle(money);
            }
            org.apache.poi.ss.usermodel.Row total=sheet.createRow(r+1); total.createCell(2).setCellValue("Cộng phát sinh"); total.createCell(3).setCellValue(report.increase().doubleValue()); total.getCell(3).setCellStyle(money); total.createCell(4).setCellValue(report.decrease().doubleValue()); total.getCell(4).setCellStyle(money);
            org.apache.poi.ss.usermodel.Row closing=sheet.createRow(r+2); closing.createCell(2).setCellValue("Dư cuối kỳ"); closing.createCell(5).setCellValue(report.closing().doubleValue()); closing.getCell(5).setCellStyle(money);
            for(int i=0;i<headers.length;i++) sheet.autoSizeColumn(i);
            wb.write(out); return out.toByteArray();
        }
    }

    public byte[] exportPdf(Report report) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 28, 32);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            BaseFont bf = BaseFont.createFont("liberation/LiberationSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            BaseFont bfb = BaseFont.createFont("liberation/LiberationSans-Bold.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            com.lowagie.text.Font normal = new com.lowagie.text.Font(bf, 9), bold = new com.lowagie.text.Font(bfb, 9), heading = new com.lowagie.text.Font(bfb, 16);
            doc.add(new Paragraph("SỔ CHI TIẾT CÔNG NỢ ĐẠI LÝ", heading));
            doc.add(new Paragraph("Đại lý: " + report.customerCode() + " - " + report.customerName(), normal));
            doc.add(new Paragraph("Kỳ đối chiếu: " + report.from() + " đến " + report.to(), normal));
            doc.add(new Paragraph("Dư đầu kỳ: " + money(report.opening()) + "    Phát sinh tăng: " + money(report.increase()) +
                    "    Phát sinh giảm: " + money(report.decrease()) + "    Dư cuối kỳ: " + money(report.closing()), bold));
            PdfPTable table = new PdfPTable(new float[]{65, 80, 200, 90, 90, 95}); table.setWidthPercentage(100); table.setHeaderRows(1); table.setSpacingBefore(10);
            for(String h:new String[]{"Ngày","Chứng từ","Diễn giải","Phát sinh tăng","Phát sinh giảm","Số dư lũy kế"}) { PdfPCell cell=new PdfPCell(new Phrase(h,bold)); cell.setBackgroundColor(new Color(232,237,243)); cell.setPadding(5); table.addCell(cell); }
            for(Entry e:report.entries()) {
                table.addCell(new Phrase(e.date().toString(),normal)); table.addCell(new Phrase(e.reference(),normal)); table.addCell(new Phrase(e.type()+" - "+e.note(),normal));
                addMoneyCell(table,e.increase(),normal); addMoneyCell(table,e.decrease(),normal); addMoneyCell(table,e.balance(),normal);
            }
            doc.add(table); doc.close(); return out.toByteArray();
        } catch (DocumentException e) { throw new IOException("Không tạo được PDF sổ công nợ",e); }
        finally { if(doc.isOpen()) doc.close(); }
    }

    private static void addMoneyCell(PdfPTable table, BigDecimal value, com.lowagie.text.Font font) { PdfPCell c=new PdfPCell(new Phrase(money(value),font)); c.setHorizontalAlignment(Element.ALIGN_RIGHT); c.setPadding(4); table.addCell(c); }
    private static String money(BigDecimal value) { return new java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("vi-VN"))).format(value) + " ₫"; }
    private record RawEntry(LocalDate date, String reference, String type, String note, BigDecimal increase, BigDecimal decrease) {}
}
