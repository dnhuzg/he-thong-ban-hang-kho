
package com.oms.service;

import com.oms.dao.DeliveryTripDao;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryTripServiceTest extends DatabaseTest {

    private final DeliveryTripService service = new DeliveryTripService();

    /**
     * Tạo dữ liệu: hai khách hàng cùng khu vực,
     * hai phiếu xuất READY ở Kho A.
     */
    private void seedShipments() throws SQLException {
        sql("""
                INSERT INTO customers (id, code, name, area_id)
                VALUES
                    (101, 'KH101', 'Khách hàng Một', 30),
                    (102, 'KH102', 'Khách hàng Hai', 30)
                """);

        sql("""
                INSERT INTO customer_delivery_points
                    (id, customer_id, address, receiver_name,
                     receiver_phone, route_note, is_default)
                VALUES
                    (201, 101, 'Số 10 đường A', 'Nguyễn Văn A',
                     '0901111111', 'Giao buổi sáng', TRUE),
                    (202, 102, 'Số 20 đường B', 'Trần Văn B',
                     '0902222222', 'Giao buổi chiều', TRUE)
                """);

        sql("""
                INSERT INTO orders
                    (id, code, customer_id, delivery_point_id, warehouse_id)
                VALUES
                    (301, 'DH301', 101, 201, 10),
                    (302, 'DH302', 102, 202, 10)
                """);

        sql("""
                INSERT INTO shipments
                    (id, code, warehouse_id, customer_id, status, order_id)
                VALUES
                    (401, 'PX401', 10, 101, 'READY', 301),
                    (402, 'PX402', 10, 102, 'READY', 302)
                """);
    }

    private void seedDriverAndVehicle() throws SQLException {
        sql("""
                INSERT INTO delivery_drivers
                    (id, full_name, phone, is_active)
                VALUES (501, 'Nguyễn Văn Tài', '0912345678', TRUE)
                """);

        sql("""
                INSERT INTO delivery_vehicles
                    (id, license_plate, vehicle_type, is_active)
                VALUES (601, '20C-12345', 'Xe tải', TRUE)
                """);
    }

    /**
     * AC1: Gom nhiều phiếu xuất cùng kho, cùng khu vực
     * vào một chuyến giao.
     */
    @Test
    void createTripWithMultipleShipmentsInSameArea()
            throws SQLException {

        User admin = user("trip_admin_1", Role.ADMIN);
        seedShipments();

        long tripId = service.create(
                List.of(401L, 402L),
                LocalDate.of(2026, 10, 17),
                null,
                null,
                admin
        );

        DeliveryTripService.Detail detail =
                service.get(tripId, admin);

        assertNotNull(detail);
        assertEquals("CG000001", detail.trip().code());
        assertEquals(10L, detail.trip().warehouseId());
        assertEquals("Địa bàn A", detail.trip().areaName());
        assertEquals("PLANNED", detail.trip().status());

        assertEquals(2, detail.stops().size());
        assertEquals(401L, detail.stops().get(0).shipmentId());
        assertEquals(402L, detail.stops().get(1).shipmentId());

        assertEquals(
                2L,
                count(
                        "delivery_trip_shipments",
                        "trip_id = " + tripId
                )
        );
    }

    /**
     * AC1: Không cho phân một phiếu xuất vào hai chuyến.
     */
    @Test
    void rejectShipmentAlreadyAssigned()
            throws SQLException {

        User admin = user("trip_admin_2", Role.ADMIN);
        seedShipments();

        service.create(
                List.of(401L),
                LocalDate.of(2026, 10, 17),
                null,
                null,
                admin
        );

        assertThrows(
                ServiceException.class,
                () -> service.create(
                        List.of(401L),
                        LocalDate.of(2026, 10, 18),
                        null,
                        null,
                        admin
                )
        );

        assertEquals(
                1L,
                count(
                        "delivery_trip_shipments",
                        "shipment_id = 401"
                )
        );
    }

    /**
     * AC1: Không được gom các phiếu khác khu vực.
     * Khi thất bại không được tạo chuyến dở dang.
     */
    @Test
    void rejectShipmentsFromDifferentAreas()
            throws SQLException {

        User admin = user("trip_admin_3", Role.ADMIN);
        seedShipments();

        sql("""
                INSERT INTO areas (id, name)
                VALUES (31, 'Địa bàn B')
                """);

        sql("""
                UPDATE customers
                SET area_id = 31
                WHERE id = 102
                """);

        assertThrows(
                ServiceException.class,
                () -> service.create(
                        List.of(401L, 402L),
                        LocalDate.of(2026, 10, 17),
                        null,
                        null,
                        admin
                )
        );

        assertEquals(0L, count("delivery_trips", "1 = 1"));
        assertEquals(
                0L,
                count("delivery_trip_shipments", "1 = 1")
        );
    }

    /**
     * AC2: Gán tài xế và phương tiện cho chuyến giao.
     */
    @Test
    void assignDriverAndVehicle()
            throws SQLException {

        User admin = user("trip_admin_4", Role.ADMIN);
        seedShipments();
        seedDriverAndVehicle();

        long tripId = service.create(
                List.of(401L, 402L),
                LocalDate.of(2026, 10, 17),
                null,
                null,
                admin
        );

        service.assign(tripId, 501L, 601L, admin);

        DeliveryTripService.Detail detail =
                service.get(tripId, admin);

        assertEquals(501L, detail.trip().driverId());
        assertEquals(601L, detail.trip().vehicleId());
        assertEquals(
                "Nguyễn Văn Tài",
                detail.trip().driverName()
        );
        assertEquals(
                "20C-12345",
                detail.trip().licensePlate()
        );
    }

    /**
     * AC2: Từ chối tài xế đã ngừng hoạt động.
     */
    @Test
    void rejectInactiveDriver()
            throws SQLException {

        User admin = user("trip_admin_5", Role.ADMIN);
        seedShipments();
        seedDriverAndVehicle();

        sql("""
                UPDATE delivery_drivers
                SET is_active = FALSE
                WHERE id = 501
                """);

        long tripId = service.create(
                List.of(401L),
                LocalDate.of(2026, 10, 17),
                null,
                null,
                admin
        );

        assertThrows(
                ServiceException.class,
                () -> service.assign(
                        tripId, 501L, 601L, admin
                )
        );

        assertNull(service.get(tripId, admin).trip().driverId());
    }

    /**
     * AC3: Backend trả đủ thông tin danh sách điểm giao
     * để sử dụng khi in.
     */
    @Test
    void returnDeliveryStopsWithAddressAndReceiverPhone()
            throws SQLException {

        User admin = user("trip_admin_6", Role.ADMIN);
        seedShipments();

        long tripId = service.create(
                List.of(401L, 402L),
                LocalDate.of(2026, 10, 17),
                null,
                null,
                admin
        );

        List<DeliveryTripDao.ShipmentStop> stops =
                service.get(tripId, admin).stops();

        assertEquals(2, stops.size());

        DeliveryTripDao.ShipmentStop first = stops.get(0);
        assertEquals(1, first.stopOrder());
        assertEquals("Số 10 đường A", first.address());
        assertEquals("Nguyễn Văn A", first.receiverName());
        assertEquals("0901111111", first.receiverPhone());
        assertEquals("Giao buổi sáng", first.routeNote());

        DeliveryTripDao.ShipmentStop second = stops.get(1);
        assertEquals(2, second.stopOrder());
        assertEquals("Số 20 đường B", second.address());
        assertEquals("Trần Văn B", second.receiverName());
        assertEquals("0902222222", second.receiverPhone());
        assertEquals("Giao buổi chiều", second.routeNote());
    }

    /**
     * GHTK-64: chuyến còn điểm chờ giao, tiền hàng chưa giao tính theo giá thuần trên đơn,
     * quá ngày giao thì cảnh báo; giao hết hoặc nhận lại hết thì chuyến rời danh sách.
     */
    @Test
    void inTransitCountsRemainingStopsValueAndOverdueTrips()
            throws SQLException {

        User admin = user("trip_admin_transit", Role.ADMIN);
        seedShipments();
        sql("INSERT INTO products (id, sku, name, base_unit_id) VALUES (901, 'COCA', 'Coca lon', 1)");
        sql("INSERT INTO product_units (product_id, unit_id, factor) VALUES (901, 2, 24)");
        // Đơn 301: 2 thùng = 48 lon, 480.000 trừ chiết khấu 48.000 → 9.000/lon; đơn 302: 10 lon, 10.000/lon
        sql("""
                INSERT INTO order_lines (order_id, product_id, unit_id, quantity, factor, base_quantity,
                                         gross_amount, discount_amount)
                VALUES (301, 901, 2, 2, 24, 48, 480000, 48000),
                       (302, 901, 1, 10, 1, 10, 100000, NULL)
                """);
        sql("""
                INSERT INTO shipment_items (shipment_id, product_id, unit_id, quantity)
                VALUES (401, 901, 2, 1), (402, 901, 1, 10)
                """);
        LocalDate day = LocalDate.of(2026, 10, 17);
        service.create(List.of(401L, 402L), day, null, null, admin);

        DeliveryTripService.InTransitPage page = service.inTransit(admin, day);
        assertEquals(1, page.rows().size());
        assertEquals(2, page.remainingStops());
        assertEquals(0, new BigDecimal("316000").compareTo(page.totalValue()));   // 24 × 9.000 + 10 × 10.000
        assertFalse(page.rows().get(0).overdue());

        sql("UPDATE shipments SET status = 'DELIVERED' WHERE id = 402");
        page = service.inTransit(admin, day.plusDays(1));
        DeliveryTripDao.InTransit row = page.rows().get(0).row();
        assertEquals(1, row.delivered());
        assertEquals(1, row.getRemaining());
        assertEquals(0, new BigDecimal("216000").compareTo(page.totalValue()));
        assertTrue(page.rows().get(0).overdue());
        assertEquals(1, page.overdueTrips());

        User otherWarehouse = user("kho_b_transit", Role.WAREHOUSE);
        otherWarehouse.setWarehouses(List.of(new NamedItem(20, "Kho B")));
        assertTrue(service.inTransit(otherWarehouse, day).rows().isEmpty());
        User rep = user("nvkd_transit", Role.SALES_REP);
        assertThrows(ForbiddenException.class, () -> service.inTransit(rep, day));

        sql("UPDATE shipments SET status = 'FAILED_RETURNED' WHERE id = 401");
        assertTrue(service.inTransit(admin, day).rows().isEmpty());
    }
}
