
package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * GHTK-63 / S6-06: Truy vấn và lưu dữ liệu phân tuyến giao hàng.
 */
public class DeliveryTripDao {

    public record Driver(
            long id,
            String fullName,
            String phone
    ) {
        public long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPhone() { return phone; }
    }

    public record Vehicle(
            long id,
            String licensePlate,
            String vehicleType
    ) {
        public long getId() { return id; }
        public String getLicensePlate() { return licensePlate; }
        public String getVehicleType() { return vehicleType; }
    }

    public record Trip(
            long id,
            String code,
            long warehouseId,
            String warehouseName,
            String areaName,
            LocalDate deliveryDate,
            Long driverId,
            String driverName,
            Long vehicleId,
            String licensePlate,
            String status
    ) {
        public long getId() { return id; }
        public String getCode() { return code; }
        public long getWarehouseId() { return warehouseId; }
        public String getWarehouseName() { return warehouseName; }
        public String getAreaName() { return areaName; }
        public LocalDate getDeliveryDate() { return deliveryDate; }
        public Long getDriverId() { return driverId; }
        public String getDriverName() { return driverName; }
        public Long getVehicleId() { return vehicleId; }
        public String getLicensePlate() { return licensePlate; }
        public String getStatus() { return status; }
    }

    public record ShipmentStop(
            long shipmentId,
            String shipmentCode,
            long warehouseId,
            String warehouseName,
            String areaName,
            String customerName,
            String address,
            String receiverName,
            String receiverPhone,
            String routeNote,
            int stopOrder
    ) {
        public long getShipmentId() { return shipmentId; }
        public String getShipmentCode() { return shipmentCode; }
        public long getWarehouseId() { return warehouseId; }
        public String getWarehouseName() { return warehouseName; }
        public String getAreaName() { return areaName; }
        public String getCustomerName() { return customerName; }
        public String getAddress() { return address; }
        public String getReceiverName() { return receiverName; }
        public String getReceiverPhone() { return receiverPhone; }
        public String getRouteNote() { return routeNote; }
        public int getStopOrder() { return stopOrder; }
    }

    private static final String TRIP_COLUMNS = """
            t.id, t.code, t.warehouse_id,
            w.name AS warehouse_name,
            t.area_name, t.delivery_date,
            t.driver_id, d.full_name AS driver_name,
            t.vehicle_id, v.license_plate,
            t.status
            """;

    private static final String TRIP_JOINS = """
            FROM delivery_trips t
            JOIN warehouses w ON w.id = t.warehouse_id
            LEFT JOIN delivery_drivers d ON d.id = t.driver_id
            LEFT JOIN delivery_vehicles v ON v.id = t.vehicle_id
            """;

    /**
     * Lấy các phiếu giao READY chưa được phân chuyến.
     * Ưu tiên điểm giao đã chọn trong đơn hàng.
     */
    public List<ShipmentStop> availableShipments(Connection conn)
            throws SQLException {

        String sql = """
                SELECT s.id AS shipment_id,
                       s.code AS shipment_code,
                       s.warehouse_id,
                       w.name AS warehouse_name,
                       a.name AS area_name,
                       c.name AS customer_name,
                       dp.address,
                       dp.receiver_name,
                       dp.receiver_phone,
                       dp.route_note
                FROM shipments s
                JOIN warehouses w ON w.id = s.warehouse_id
                JOIN customers c ON c.id = s.customer_id
                JOIN areas a ON a.id = c.area_id
                LEFT JOIN orders o ON o.id = s.order_id
                JOIN customer_delivery_points dp
                  ON dp.id = COALESCE(
                      o.delivery_point_id,
                      (
                          SELECT MIN(p.id)
                          FROM customer_delivery_points p
                          WHERE p.customer_id = s.customer_id
                            AND p.is_default = TRUE
                      )
                  )
                WHERE s.status = 'READY'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM delivery_trip_shipments ts
                      WHERE ts.shipment_id = s.id
                  )
                ORDER BY s.warehouse_id, a.name, s.id
                """;

        List<ShipmentStop> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapStop(rs, 0));
            }
        }

        return result;
    }

    /**
     * Khóa phiếu giao để tránh hai nhân viên cùng phân chuyến.
     */
    public ShipmentStop lockShipment(Connection conn, long shipmentId)
            throws SQLException {

        String sql = """
                SELECT s.id AS shipment_id,
                       s.code AS shipment_code,
                       s.warehouse_id,
                       w.name AS warehouse_name,
                       a.name AS area_name,
                       c.name AS customer_name,
                       dp.address,
                       dp.receiver_name,
                       dp.receiver_phone,
                       dp.route_note
                FROM shipments s
                JOIN warehouses w ON w.id = s.warehouse_id
                JOIN customers c ON c.id = s.customer_id
                JOIN areas a ON a.id = c.area_id
                LEFT JOIN orders o ON o.id = s.order_id
                LEFT JOIN customer_delivery_points dp
                  ON dp.id = COALESCE(
                      o.delivery_point_id,
                      (
                          SELECT MIN(p.id)
                          FROM customer_delivery_points p
                          WHERE p.customer_id = s.customer_id
                            AND p.is_default = TRUE
                      )
                  )
                WHERE s.id = ?
                FOR UPDATE
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shipmentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapStop(rs, 0);
            }
        }
    }

    public List<Driver> activeDrivers(Connection conn)
            throws SQLException {

        List<Driver> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT id, full_name, phone
                FROM delivery_drivers
                WHERE is_active = TRUE
                ORDER BY full_name, id
                """);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new Driver(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("phone")
                ));
            }
        }

        return result;
    }

    public List<Vehicle> activeVehicles(Connection conn)
            throws SQLException {

        List<Vehicle> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT id, license_plate, vehicle_type
                FROM delivery_vehicles
                WHERE is_active = TRUE
                ORDER BY license_plate, id
                """);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new Vehicle(
                        rs.getLong("id"),
                        rs.getString("license_plate"),
                        rs.getString("vehicle_type")
                ));
            }
        }

        return result;
    }

    public boolean activeDriverExists(Connection conn, long driverId)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT 1
                FROM delivery_drivers
                WHERE id = ? AND is_active = TRUE
                """)) {

            ps.setLong(1, driverId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean activeVehicleExists(Connection conn, long vehicleId)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT 1
                FROM delivery_vehicles
                WHERE id = ? AND is_active = TRUE
                """)) {

            ps.setLong(1, vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertTrip(
            Connection conn,
            String code,
            long warehouseId,
            String areaName,
            LocalDate deliveryDate,
            Long driverId,
            Long vehicleId,
            long createdBy
    ) throws SQLException {

        String sql = """
                INSERT INTO delivery_trips
                    (code, warehouse_id, area_name, delivery_date,
                     driver_id, vehicle_id, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, code);
            ps.setLong(2, warehouseId);
            ps.setString(3, areaName);
            ps.setDate(4, Date.valueOf(deliveryDate));

            if (driverId == null) {
                ps.setNull(5, java.sql.Types.BIGINT);
            } else {
                ps.setLong(5, driverId);
            }

            if (vehicleId == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, vehicleId);
            }

            ps.setLong(7, createdBy);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException(
                            "Không lấy được mã chuyến giao hàng.");
                }
                return rs.getLong(1);
            }
        }
    }

    public void updateCode(Connection conn, long tripId, String code)
            throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE delivery_trips SET code = ? WHERE id = ?")) {

            ps.setString(1, code);
            ps.setLong(2, tripId);
            ps.executeUpdate();
        }
    }

    public void addShipment(
            Connection conn,
            long tripId,
            long shipmentId,
            int stopOrder
    ) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                """
                INSERT INTO delivery_trip_shipments
                    (trip_id, shipment_id, stop_order)
                VALUES (?, ?, ?)
                """)) {

            ps.setLong(1, tripId);
            ps.setLong(2, shipmentId);
            ps.setInt(3, stopOrder);
            ps.executeUpdate();
        }
    }

    public boolean shipmentAlreadyAssigned(
            Connection conn, long shipmentId
    ) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                """
                SELECT 1
                FROM delivery_trip_shipments
                WHERE shipment_id = ?
                """)) {

            ps.setLong(1, shipmentId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Trip findTrip(Connection conn, long tripId)
            throws SQLException {

        String sql = "SELECT " + TRIP_COLUMNS
                + TRIP_JOINS
                + "WHERE t.id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapTrip(rs) : null;
            }
        }
    }

    public List<Trip> listTrips(Connection conn)
            throws SQLException {

        String sql = "SELECT " + TRIP_COLUMNS
                + TRIP_JOINS
                + "ORDER BY t.delivery_date DESC, t.id DESC";

        List<Trip> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapTrip(rs));
            }
        }

        return result;
    }

    /** GHTK-64: chuyến còn điểm chờ giao, kèm số điểm và giá trị hàng chưa giao. */
    public record InTransit(Trip trip, int stops, int delivered, int failed, BigDecimal remainingValue) {
        public Trip getTrip() { return trip; }
        public int getStops() { return stops; }
        public int getDelivered() { return delivered; }
        public int getFailed() { return failed; }
        public int getRemaining() { return stops - delivered - failed; }
        public BigDecimal getRemainingValue() { return remainingValue; }
    }

    /**
     * Giá trị một phiếu tính như lúc lập hoá đơn: số lượng quy về đơn vị cơ sở
     * × đơn giá thuần (sau chiết khấu) của sản phẩm trên đơn.
     */
    public List<InTransit> inTransit(Connection conn) throws SQLException {
        String sql = "SELECT " + TRIP_COLUMNS + """
                , COUNT(*) AS stops,
                SUM(CASE WHEN sv.status = 'DELIVERED' THEN 1 ELSE 0 END) AS delivered,
                SUM(CASE WHEN sv.status = 'FAILED_RETURNED' THEN 1 ELSE 0 END) AS failed,
                SUM(CASE WHEN sv.status = 'READY' THEN COALESCE(sv.amount, 0) ELSE 0 END) AS remaining_value
                """ + TRIP_JOINS + """
                JOIN delivery_trip_shipments ts ON ts.trip_id = t.id
                JOIN (
                    SELECT s.id, s.status,
                        (SELECT SUM(i.quantity
                                * CASE WHEN i.unit_id = p.base_unit_id THEN 1 ELSE
                                    (SELECT pu.factor FROM product_units pu
                                     WHERE pu.product_id = i.product_id AND pu.unit_id = i.unit_id) END
                                * (SELECT SUM(l.gross_amount - COALESCE(l.discount_amount, 0)) / SUM(l.base_quantity)
                                   FROM order_lines l
                                   WHERE l.order_id = s.order_id AND l.product_id = i.product_id))
                         FROM shipment_items i JOIN products p ON p.id = i.product_id
                         WHERE i.shipment_id = s.id) AS amount
                    FROM shipments s
                ) sv ON sv.id = ts.shipment_id
                GROUP BY t.id, t.code, t.warehouse_id, w.name, t.area_name, t.delivery_date,
                    t.driver_id, d.full_name, t.vehicle_id, v.license_plate, t.status
                HAVING SUM(CASE WHEN sv.status = 'READY' THEN 1 ELSE 0 END) > 0
                ORDER BY t.delivery_date, t.id
                """;

        List<InTransit> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new InTransit(
                        mapTrip(rs),
                        rs.getInt("stops"),
                        rs.getInt("delivered"),
                        rs.getInt("failed"),
                        rs.getBigDecimal("remaining_value")
                ));
            }
        }

        return result;
    }

    /**
     * Danh sách điểm giao theo thứ tự ghé thăm.
     * Dùng cho trang chi tiết và bản in chuyến giao.
     */
    public List<ShipmentStop> tripStops(
            Connection conn, long tripId
    ) throws SQLException {

        String sql = """
                SELECT s.id AS shipment_id,
                       s.code AS shipment_code,
                       s.warehouse_id,
                       w.name AS warehouse_name,
                       a.name AS area_name,
                       c.name AS customer_name,
                       dp.address,
                       dp.receiver_name,
                       dp.receiver_phone,
                       dp.route_note,
                       ts.stop_order
                FROM delivery_trip_shipments ts
                JOIN shipments s ON s.id = ts.shipment_id
                JOIN warehouses w ON w.id = s.warehouse_id
                JOIN customers c ON c.id = s.customer_id
                JOIN areas a ON a.id = c.area_id
                LEFT JOIN orders o ON o.id = s.order_id
                LEFT JOIN customer_delivery_points dp
                  ON dp.id = COALESCE(
                      o.delivery_point_id,
                      (
                          SELECT MIN(p.id)
                          FROM customer_delivery_points p
                          WHERE p.customer_id = s.customer_id
                            AND p.is_default = TRUE
                      )
                  )
                WHERE ts.trip_id = ?
                ORDER BY ts.stop_order, s.id
                """;

        List<ShipmentStop> result = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapStop(
                            rs,
                            rs.getInt("stop_order")
                    ));
                }
            }
        }

        return result;
    }

    public void assignDriverAndVehicle(
            Connection conn,
            long tripId,
            long driverId,
            long vehicleId
    ) throws SQLException {

        try (PreparedStatement ps = conn.prepareStatement(
                """
                UPDATE delivery_trips
                SET driver_id = ?, vehicle_id = ?
                WHERE id = ? AND status = 'PLANNED'
                """)) {

            ps.setLong(1, driverId);
            ps.setLong(2, vehicleId);
            ps.setLong(3, tripId);

            if (ps.executeUpdate() != 1) {
                throw new SQLException(
                        "Không thể gán tài xế và phương tiện cho chuyến.");
            }
        }
    }

    private static Trip mapTrip(ResultSet rs)
            throws SQLException {

        long driver = rs.getLong("driver_id");
        Long driverId = rs.wasNull() ? null : driver;

        long vehicle = rs.getLong("vehicle_id");
        Long vehicleId = rs.wasNull() ? null : vehicle;

        Date date = rs.getDate("delivery_date");

        return new Trip(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getLong("warehouse_id"),
                rs.getString("warehouse_name"),
                rs.getString("area_name"),
                date == null ? null : date.toLocalDate(),
                driverId,
                rs.getString("driver_name"),
                vehicleId,
                rs.getString("license_plate"),
                rs.getString("status")
        );
    }

    private static ShipmentStop mapStop(
            ResultSet rs, int stopOrder
    ) throws SQLException {

        return new ShipmentStop(
                rs.getLong("shipment_id"),
                rs.getString("shipment_code"),
                rs.getLong("warehouse_id"),
                rs.getString("warehouse_name"),
                rs.getString("area_name"),
                rs.getString("customer_name"),
                rs.getString("address"),
                rs.getString("receiver_name"),
                rs.getString("receiver_phone"),
                rs.getString("route_note"),
                stopOrder
        );
    }
}
