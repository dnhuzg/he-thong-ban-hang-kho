package com.oms.service;
import com.oms.model.Order;
import com.oms.model.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/** PDF do server tạo: cùng phạm vi đại lý và cùng giá đã chốt với các endpoint đơn hàng. */
public class OrderPdfService {
    public byte[] export(long id,User viewer) throws SQLException,IOException {
        return render(new OrderService().get(id,viewer));
    }
    byte[] render(Order order) throws IOException {
        Font normal=new Font(BaseFont.createFont("liberation/LiberationSans-Regular.ttf",BaseFont.IDENTITY_H,BaseFont.EMBEDDED),10);
        Font bold=new Font(BaseFont.createFont("liberation/LiberationSans-Bold.ttf",BaseFont.IDENTITY_H,BaseFont.EMBEDDED),10);
        var output=new ByteArrayOutputStream();
        Document document=new Document(PageSize.A4,36,36,36,42);
        try {
            PdfWriter writer=PdfWriter.getInstance(document,output);
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override public void onEndPage(PdfWriter w,Document d) {
                    ColumnText.showTextAligned(w.getDirectContent(),Element.ALIGN_RIGHT,
                            new Phrase("Trang "+w.getPageNumber(),normal),d.right(),24,0);
                }
            });
            document.open();
            document.add(new Paragraph("ĐƠN HÀNG "+order.getCode(),new Font(bold.getBaseFont(),18)));
            document.add(new Paragraph("Đại lý: "+order.getCustomerCode()+" – "+order.getCustomerName(),normal));
            document.add(new Paragraph("Điểm giao: "+text(order.getDeliveryPointText()),normal));
            document.add(new Paragraph("Ngày giao mong muốn: "+text(order.getDeliveryDateText()),normal));
            document.add(new Paragraph("Người lập: "+text(order.getCreatedByName()),normal));
            if(order.getStatus()==Order.Status.DRAFT) document.add(new Paragraph("BẢN NHÁP — giá được tính theo bảng giá hiện hành.",bold));
            if(order.getNote()!=null) document.add(new Paragraph("Ghi chú: "+order.getNote(),normal));
            Barcode128 barcode=new Barcode128(); barcode.setCodeType(Barcode.CODE128); barcode.setCode(order.getCode()); barcode.setBarHeight(32);
            var barcodeImage=barcode.createImageWithBarcode(writer.getDirectContent(),Color.BLACK,Color.BLACK);
            barcodeImage.setSpacingBefore(10); barcodeImage.setSpacingAfter(12); document.add(barcodeImage);
            PdfPTable table=new PdfPTable(new float[]{55,160,35,80,65,85}); table.setWidthPercentage(100); table.setHeaderRows(1);
            for(String header:new String[]{"SKU","Sản phẩm / ĐVT","SL","Đơn giá","Chiết khấu","Thành tiền"}) {
                PdfPCell cell=new PdfPCell(new Phrase(header,bold)); cell.setBackgroundColor(new Color(235,239,244)); cell.setPadding(6); table.addCell(cell);
            }
            for(var line:order.getLines()) {
                String[] cells={line.getSku(),line.getProductName()+" / "+line.getUnitName(),Integer.toString(line.getQuantity()),
                        money(line.getUnitPrice()),money(line.getDiscountAmount()),money(line.getNetAmount())};
                for(int i=0;i<cells.length;i++) {
                    PdfPCell cell=new PdfPCell(new Phrase(cells[i],normal)); cell.setPadding(5);
                    if(i>=2) cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cell);
                }
            }
            document.add(table);
            document.add(new Paragraph("Tiền hàng: "+money(order.getGrossTotal())+" ₫",normal));
            document.add(new Paragraph("Chiết khấu: "+money(order.getDiscountTotal())+" ₫",normal));
            document.add(new Paragraph("TỔNG TIỀN: "+money(order.getPayable())+" ₫",bold));
            if(order.getUnpricedLines()>0) document.add(new Paragraph("Có "+order.getUnpricedLines()+" dòng chưa có giá; tổng tiền chưa bao gồm các dòng này.",bold));
            document.close(); return output.toByteArray();
        } catch(DocumentException e) { throw new IOException("Không tạo được PDF đơn hàng",e); }
        finally { if(document.isOpen()) document.close(); }
    }
    private static String text(String value) { return value==null || value.isBlank()?"Chưa chọn":value; }
    private static String money(BigDecimal value) { return value==null?"Chưa có giá":new java.text.DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.forLanguageTag("vi-VN"))).format(value); }
}
