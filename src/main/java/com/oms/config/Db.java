package com.oms.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Thiếu MySQL JDBC driver", e);
        }
    }

    private Db() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(AppConfig.get("db.url"), AppConfig.get("db.user"), AppConfig.get("db.password"));
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection conn) throws SQLException;
    }

    /** Chạy nhiều câu lệnh trong một transaction; lỗi thì rollback toàn bộ. */
    public static <T> T inTransaction(TxWork<T> work) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
