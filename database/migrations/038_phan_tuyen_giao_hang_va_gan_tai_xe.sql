-- GHTK-63 / S6-06: Phan tuyen giao hang va gan tai xe
USE oms;

CREATE TABLE IF NOT EXISTS delivery_drivers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    license_no VARCHAR(50) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    license_plate VARCHAR(30) NOT NULL,
    vehicle_type VARCHAR(100) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_vehicles_plate (license_plate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_trips (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    warehouse_id BIGINT NOT NULL,
    area_name VARCHAR(150) NOT NULL,
    delivery_date DATE NOT NULL,
    driver_id BIGINT NULL,
    vehicle_id BIGINT NULL,
    status ENUM('PLANNED', 'IN_PROGRESS', 'COMPLETED')
        NOT NULL DEFAULT 'PLANNED',
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_trips_code (code),
    KEY idx_delivery_trips_warehouse_date (warehouse_id, delivery_date),
    KEY idx_delivery_trips_driver (driver_id),
    KEY idx_delivery_trips_vehicle (vehicle_id),
    CONSTRAINT fk_delivery_trips_warehouse
        FOREIGN KEY (warehouse_id) REFERENCES warehouses (id),
    CONSTRAINT fk_delivery_trips_driver
        FOREIGN KEY (driver_id) REFERENCES delivery_drivers (id),
    CONSTRAINT fk_delivery_trips_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES delivery_vehicles (id),
    CONSTRAINT fk_delivery_trips_creator
        FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS delivery_trip_shipments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trip_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    stop_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_trip_shipment (shipment_id),
    UNIQUE KEY uk_delivery_trip_stop (trip_id, stop_order),
    KEY idx_delivery_trip_shipments_trip (trip_id),
    CONSTRAINT fk_delivery_trip_shipments_trip
        FOREIGN KEY (trip_id) REFERENCES delivery_trips (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_delivery_trip_shipments_shipment
        FOREIGN KEY (shipment_id) REFERENCES shipments (id),
    CONSTRAINT chk_delivery_trip_stop_order
        CHECK (stop_order > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;