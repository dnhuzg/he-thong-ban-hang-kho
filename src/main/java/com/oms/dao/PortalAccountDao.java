package com.oms.dao;
import java.sql.Connection;
import java.sql.SQLException;

/** Quan hệ tài khoản được quản trị cấp sẵn; API cổng không cho người dùng tự đổi đại lý. */
public class PortalAccountDao {
    public Long customerId(Connection conn,long userId) throws SQLException {
        try(var ps=conn.prepareStatement("SELECT customer_id FROM customer_accounts WHERE user_id=?")) {
            ps.setLong(1,userId); try(var rs=ps.executeQuery()) { return rs.next()?rs.getLong(1):null; }
        }
    }
    public boolean owns(Connection conn,long customerId,long userId) throws SQLException {
        Long own=customerId(conn,userId); return own!=null && own==customerId;
    }
}
