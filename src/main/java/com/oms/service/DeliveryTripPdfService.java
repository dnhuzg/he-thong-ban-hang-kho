
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
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ColumnText;
import com.oms.dao.DeliveryTripDao;
import com.oms.model.User;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * GHTK-63 / S6-06 - AC3:
 * Xuất PDF danh sách điểm giao của một chuyến.
 */
public class DeliveryTripPdfService {

    private final DeliveryTripService tripService =
            new DeliveryTripService();

    /**
     * Kiểm tra quyền xem chuyến trước khi tạo PDF.
     */
    public byte[] export(long tripId, User viewer)
            throws SQLException, IOException {

        DeliveryTripService.Detail detail =
                tripService.get(tripId, viewer);

        return render(detail);
    }

    /**
     * Tạo PDF A4 với đầy đủ địa chỉ, số điện thoại người nhận.
     */
    byte[] render(DeliveryTripService.Detail detail)
            throws IOException {

        BaseFont regularFont = BaseFont.createFont(
                "liberation/LiberationSans-Regular.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
        );

        BaseFont boldFont = BaseFont.createFont(
                "liberation/LiberationSans-Bold.ttf",
                BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED
        );

        Font normal = new Font(regularFont, 9);
        Font bold = new Font(boldFont, 9);
        Font title = new Font(boldFont, 16);

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        Document document = new Document(
                PageSize.A4.rotate(),
                30, 30, 36, 42
        );

        try {
            PdfWriter writer =
                    PdfWriter.getInstance(document, output);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document d) {
                    ColumnText.showTextAligned(
                            w.getDirectContent(),
                            Element.ALIGN_RIGHT,
                            new Phrase(
                                    "Trang " + w.getPageNumber(),
                                    normal
                            ),
                            d.right(),
                            22,
                            0
                    );
                }
            });

            document.open();

            DeliveryTripDao.Trip trip = detail.trip();

            Paragraph heading = new Paragraph(
                    "DANH SÁCH ĐIỂM GIAO HÀNG",
                    title
            );
            heading.setAlignment(Element.ALIGN_CENTER);
            heading.setSpacingAfter(12);
            document.add(heading);

            document.add(new Paragraph(
                    "Mã chuyến: " + text(trip.code()),
                    bold
            ));
            document.add(new Paragraph(
                    "Kho xuất: " + text(trip.warehouseName()),
                    normal
            ));
            document.add(new Paragraph(
                    "Khu vực: " + text(trip.areaName()),
                    normal
            ));
            document.add(new Paragraph(
                    "Ngày giao: " + trip.deliveryDate(),
                    normal
            ));
            document.add(new Paragraph(
                    "Tài xế: " + text(trip.driverName()),
                    normal
            ));
            document.add(new Paragraph(
                    "Phương tiện: " + text(trip.licensePlate()),
                    normal
            ));

            List<DeliveryTripDao.ShipmentStop> stops =
                    detail.stops();

            Paragraph count = new Paragraph(
                    "Tổng số điểm giao: " + stops.size(),
                    bold
            );
            count.setSpacingBefore(10);
            count.setSpacingAfter(10);
            document.add(count);

            PdfPTable table = new PdfPTable(
                    new float[]{35, 75, 100, 160, 110, 85, 125}
            );

            table.setWidthPercentage(100);
            table.setHeaderRows(1);

            for (String header : new String[]{
                    "STT",
                    "Phiếu xuất",
                    "Khách hàng",
                    "Địa chỉ giao",
                    "Người nhận",
                    "SĐT người nhận",
                    "Ghi chú"
            }) {
                PdfPCell cell =
                        new PdfPCell(new Phrase(header, bold));
                cell.setBackgroundColor(
                        new Color(235, 239, 244)
                );
                cell.setPadding(6);
                table.addCell(cell);
            }

            for (DeliveryTripDao.ShipmentStop stop : stops) {
                String[] values = {
                        String.valueOf(stop.stopOrder()),
                        text(stop.shipmentCode()),
                        text(stop.customerName()),
                        text(stop.address()),
                        text(stop.receiverName()),
                        text(stop.receiverPhone()),
                        text(stop.routeNote())
                };

                for (String value : values) {
                    PdfPCell cell =
                            new PdfPCell(new Phrase(value, normal));
                    cell.setPadding(5);
                    cell.setVerticalAlignment(Element.ALIGN_TOP);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();

            return output.toByteArray();

        } catch (DocumentException e) {
            throw new IOException(
                    "Không tạo được PDF danh sách điểm giao.",
                    e
            );
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private static String text(String value) {
        return value == null || value.isBlank()
                ? "Chưa có"
                : value;
    }
}
