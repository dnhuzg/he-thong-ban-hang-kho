package com.oms.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PasswordResetDao {

    /** Vô hiệu các link cũ chưa dùng để mỗi lúc chỉ có một link hợp lệ. */
    public void invalidateOpenTokens(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE password_reset_tokens SET used_at = NOW() WHERE user_id = ? AND used_at IS NULL")) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public void insert(Connection conn, long userId, String tokenHash, LocalDateTime expiresAt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)")) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    /** Trả về user_id nếu token còn hạn và chưa dùng; ngược lại null. */
    public Long findValidUserId(Connection conn, String tokenHash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT user_id FROM password_reset_tokens WHERE token_hash = ? AND used_at IS NULL AND expires_at > NOW()")) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : null;
            }
        }
    }

    /** Đánh dấu đã dùng; trả về false nếu token đã bị dùng trước đó (chống dùng hai lần song song). */
    public boolean markUsed(Connection conn, String tokenHash) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE password_reset_tokens SET used_at = NOW() WHERE token_hash = ? AND used_at IS NULL AND expires_at > NOW()")) {
            ps.setString(1, tokenHash);
            return ps.executeUpdate() == 1;
        }
    }
}
