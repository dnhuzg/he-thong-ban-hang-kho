
package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryTripPdfServiceTest extends DatabaseTest {

    private final DeliveryTripService tripService =
            new DeliveryTripService();

    private final DeliveryTripPdfService pdfService =
            new DeliveryTripPdfService();

    @Test
    void exportDeliveryStopsAsPdf() throws Exception {
        User admin = user("pdf_trip_admin", Role.ADMIN);

        sql("""
                INSERT INTO customers (id, code, name, area_id)
                VALUES (101, 'KH101', 'Khách hàng A', 30)
                """);

        sql("""
                INSERT INTO customer_delivery_points
                    (id, customer_id, address, receiver_name,
                     receiver_phone, route_note, is_default)
                VALUES
                    (201, 101, 'Số 10 đường A',
                     'Nguyễn Văn A', '0901111111',
                     'Giao buổi sáng', TRUE)
                """);

        sql("""
                INSERT INTO orders
                    (id, code, customer_id, delivery_point_id, warehouse_id)
                VALUES (301, 'DH301', 101, 201, 10)
                """);

        sql("""
                INSERT INTO shipments
                    (id, code, warehouse_id, customer_id, status, order_id)
                VALUES (401, 'PX401', 10, 101, 'READY', 301)
                """);

        long tripId = tripService.create(
                List.of(401L),
                LocalDate.of(2026, 10, 17),
                null,
                null,
                admin
        );

        byte[] pdf = pdfService.export(tripId, admin);

        // PDF phải được tạo thành công và có nội dung.
        assertNotNull(pdf);
        assertTrue(pdf.length > 500);

        // File phải có chữ ký PDF hợp lệ.
        String header = new String(
                pdf, 0, 5, StandardCharsets.US_ASCII
        );
        assertEquals("%PDF-", header);

        // Dữ liệu cung cấp cho PDF phải có đủ thông tin điểm giao.
        var detail = tripService.get(tripId, admin);

        assertEquals(1, detail.stops().size());
        assertEquals("Số 10 đường A", detail.stops().get(0).address());
        assertEquals("Nguyễn Văn A", detail.stops().get(0).receiverName());
        assertEquals("0901111111", detail.stops().get(0).receiverPhone());
    }

    @Test
    void rejectExportForNonexistentTrip() throws Exception {
        User admin = user("pdf_missing_trip", Role.ADMIN);

        assertThrows(
                DeliveryTripService.NotFoundException.class,
                () -> pdfService.export(9999L, admin)
        );
    }
}
