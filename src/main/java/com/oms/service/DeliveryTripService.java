
package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.DeliveryTripDao;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * GHTK-63 / S6-06: Phân tuyến giao hàng và gán tài xế.
 */
public class DeliveryTripService {

    private static final int MAX_SHIPMENTS = 100;

    private final DeliveryTripDao dao = new DeliveryTripDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public record Page(
            List<DeliveryTripDao.Trip> trips,
            List<DeliveryTripDao.ShipmentStop> availableShipments,
            List<DeliveryTripDao.Driver> drivers,
            List<DeliveryTripDao.Vehicle> vehicles
    ) {}

    public record Detail(
            DeliveryTripDao.Trip trip,
            List<DeliveryTripDao.ShipmentStop> stops
    ) {}

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy chuyến giao hàng trong kho được phân quyền.");
        }
    }

    /**
     * Danh sách chuyến giao, phiếu giao chưa phân tuyến,
     * tài xế và phương tiện đang hoạt động.
     */
    public Page page(User viewer) throws SQLException {
        Set<Long> scope = scope(viewer);

        try (Connection conn = Db.getConnection()) {
            List<DeliveryTripDao.Trip> trips = dao.listTrips(conn)
                    .stream()
                    .filter(t -> inScope(scope, t.warehouseId()))
                    .toList();

            List<DeliveryTripDao.ShipmentStop> available =
                    dao.availableShipments(conn)
                            .stream()
                            .filter(s -> inScope(scope, s.warehouseId()))
                            .toList();

            return new Page(
                    trips,
                    available,
                    dao.activeDrivers(conn),
                    dao.activeVehicles(conn)
            );
        }
    }

    /**
     * Chi tiết chuyến giao, bao gồm các điểm giao
     * theo đúng thứ tự đã phân tuyến.
     *
     * Dữ liệu này được sử dụng cho bản in AC3.
     */
    public Detail get(long tripId, User viewer) throws SQLException {
        Set<Long> scope = scope(viewer);

        try (Connection conn = Db.getConnection()) {
            DeliveryTripDao.Trip trip = dao.findTrip(conn, tripId);

            if (trip == null || !inScope(scope, trip.warehouseId())) {
                throw new NotFoundException();
            }

            return new Detail(trip, dao.tripStops(conn, tripId));
        }
    }

    /**
     * AC1 + AC2:
     * Tạo một chuyến giao gồm nhiều phiếu giao cùng kho và khu vực.
     *
     * Có thể chọn tài xế và phương tiện ngay khi tạo.
     * Nếu chưa có, có thể gán sau khi lập chuyến.
     */
    public long create(
            List<Long> shipmentIds,
            LocalDate deliveryDate,
            Long driverId,
            Long vehicleId,
            User actor
    ) throws SQLException {

        Set<Long> scope = scope(actor);

        if (shipmentIds == null || shipmentIds.isEmpty()) {
            throw new ServiceException(
                    "Chọn ít nhất một phiếu giao hàng để lập chuyến.");
        }

        if (deliveryDate == null) {
            throw new ServiceException("Vui lòng chọn ngày giao hàng.");
        }

        if (shipmentIds.size() > MAX_SHIPMENTS) {
            throw new ServiceException(
                    "Một chuyến giao tối đa " + MAX_SHIPMENTS + " phiếu.");
        }

        TreeSet<Long> ids = new TreeSet<>(shipmentIds);

        if (ids.size() != shipmentIds.size()) {
            throw new ServiceException(
                    "Danh sách phiếu giao hàng bị trùng.");
        }

        for (long id : ids) {
            if (id <= 0) {
                throw new ServiceException(
                        "Mã phiếu giao hàng không hợp lệ.");
            }
        }

        return Db.inTransaction(conn -> {
            validateAssignment(conn, driverId, vehicleId);

            List<DeliveryTripDao.ShipmentStop> shipments =
                    new ArrayList<>();

            Long warehouseId = null;
            String areaName = null;

            /*
             * Khóa từng phiếu theo thứ tự ID.
             * Tránh hai nhân viên cùng lập chuyến cho một phiếu.
             */
            for (long shipmentId : ids) {
                DeliveryTripDao.ShipmentStop shipment =
                        dao.lockShipment(conn, shipmentId);

                if (shipment == null) {
                    throw new ServiceException(
                            "Không tìm thấy phiếu giao #" + shipmentId);
                }

                if (!inScope(scope, shipment.warehouseId())) {
                    throw new NotFoundException();
                }

                if (dao.shipmentAlreadyAssigned(conn, shipmentId)) {
                    throw new ServiceException(
                            "Phiếu " + shipment.shipmentCode()
                                    + " đã được phân vào chuyến khác.");
                }

                /*
                 * lockShipment chỉ lấy thông tin phiếu.
                 * Điều kiện READY phải được kiểm tra trong transaction.
                 */
                if (!isReadyShipment(conn, shipmentId)) {
                    throw new ServiceException(
                            "Phiếu " + shipment.shipmentCode()
                                    + " không còn ở trạng thái sẵn sàng giao.");
                }

                if (shipment.address() == null
                        || shipment.address().isBlank()
                        || shipment.receiverName() == null
                        || shipment.receiverName().isBlank()
                        || shipment.receiverPhone() == null
                        || shipment.receiverPhone().isBlank()) {
                    throw new ServiceException(
                            "Phiếu " + shipment.shipmentCode()
                                    + " thiếu địa chỉ hoặc thông tin người nhận.");
                }

                if (shipment.areaName() == null
                        || shipment.areaName().isBlank()) {
                    throw new ServiceException(
                            "Phiếu " + shipment.shipmentCode()
                                    + " chưa có khu vực giao hàng.");
                }

                if (warehouseId == null) {
                    warehouseId = shipment.warehouseId();
                    areaName = shipment.areaName();
                } else if (!warehouseId.equals(shipment.warehouseId())
                        || !areaName.equals(shipment.areaName())) {
                    throw new ServiceException(
                            "Chỉ được gom các phiếu giao cùng kho và cùng khu vực.");
                }

                shipments.add(shipment);
            }

            /*
             * Tạo mã tạm duy nhất theo UUID để tránh trùng mã
             * khi nhiều người lập chuyến cùng lúc.
             */
            String temporaryCode = "TMP-"
                    + java.util.UUID.randomUUID()
                    .toString()
                    .substring(0, 24);

            long tripId = dao.insertTrip(
                    conn,
                    temporaryCode,
                    warehouseId,
                    areaName,
                    deliveryDate,
                    driverId,
                    vehicleId,
                    actor.getId()
            );

            String tripCode = String.format("CG%06d", tripId);
            dao.updateCode(conn, tripId, tripCode);

            /*
             * Giữ thứ tự phiếu được người dùng chọn.
             */
            int stopOrder = 1;

            for (long shipmentId : shipmentIds) {
                dao.addShipment(
                        conn,
                        tripId,
                        shipmentId,
                        stopOrder++
                );
            }

            auditDao.log(
                    conn,
                    actor.getId(),
                    "DELIVERY_TRIP_CREATE",
                    "DELIVERY_TRIP",
                    tripId,
                    tripCode + " · "
                            + shipments.size() + " phiếu"
                            + " · khu vực: " + areaName
            );

            return tripId;
        });
    }

    /**
     * AC2:
     * Gán hoặc đổi tài xế và phương tiện cho chuyến còn PLANNED.
     */
    public void assign(
            long tripId,
            long driverId,
            long vehicleId,
            User actor
    ) throws SQLException {

        Set<Long> scope = scope(actor);

        Db.inTransaction(conn -> {
            DeliveryTripDao.Trip trip = dao.findTrip(conn, tripId);

            if (trip == null || !inScope(scope, trip.warehouseId())) {
                throw new NotFoundException();
            }

            if (!"PLANNED".equals(trip.status())) {
                throw new ServiceException(
                        "Chỉ được gán tài xế cho chuyến đang lập kế hoạch.");
            }

            validateAssignment(conn, driverId, vehicleId);

            String before = "Tài xế: " + trip.driverName()
                    + " · Xe: " + trip.licensePlate();

            dao.assignDriverAndVehicle(
                    conn,
                    tripId,
                    driverId,
                    vehicleId
            );

            auditDao.log(
                    conn,
                    actor.getId(),
                    "DELIVERY_TRIP_ASSIGN",
                    "DELIVERY_TRIP",
                    tripId,
                    trip.code(),
                    before,
                    "Tài xế #" + driverId
                            + " · Phương tiện #" + vehicleId
            );

            return null;
        });
    }

    /**
     * Kiểm tra tài xế/phương tiện có tồn tại và đang hoạt động.
     */
    private void validateAssignment(
            Connection conn,
            Long driverId,
            Long vehicleId
    ) throws SQLException {

        if (driverId != null
                && (driverId <= 0
                || !dao.activeDriverExists(conn, driverId))) {
            throw new ServiceException(
                    "Tài xế không tồn tại hoặc đã ngừng hoạt động.");
        }

        if (vehicleId != null
                && (vehicleId <= 0
                || !dao.activeVehicleExists(conn, vehicleId))) {
            throw new ServiceException(
                    "Phương tiện không tồn tại hoặc đã ngừng hoạt động.");
        }
    }

    private boolean isReadyShipment(
            Connection conn, long shipmentId
    ) throws SQLException {

        try (var ps = conn.prepareStatement(
                "SELECT status FROM shipments WHERE id = ?")) {

            ps.setLong(1, shipmentId);

            try (var rs = ps.executeQuery()) {
                return rs.next()
                        && "READY".equals(rs.getString(1));
            }
        }
    }

    /**
     * Kiểm tra quyền theo kho, tương tự PickingService.
     */
    private static Set<Long> scope(User user) {
        if (user == null) {
            throw new ForbiddenException(
                    "Vui lòng đăng nhập để quản lý chuyến giao.");
        }

        if (user.isAdmin()) {
            return null;
        }

        if (!user.hasRole(Role.WAREHOUSE)
                && !user.hasRole(Role.WH_MANAGER)) {
            throw new ForbiddenException(
                    "Chỉ Nhân viên kho hoặc Quản lý kho được phân tuyến giao hàng.");
        }

        return user.getWarehouses()
                .stream()
                .map(NamedItem::getId)
                .collect(Collectors.toSet());
    }

    private static boolean inScope(
            Set<Long> scope, long warehouseId
    ) {
        return scope == null || scope.contains(warehouseId);
    }
}
