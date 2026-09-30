package com.oms.service;

import com.oms.config.AppConfig;
import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.PasswordResetDao;
import com.oms.dao.UserDao;
import com.oms.model.User;
import com.oms.security.Passwords;
import com.oms.security.Tokens;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Quên mật khẩu qua email (S1-03): link hiệu lực 30 phút, chỉ dùng một lần. */
public class PasswordResetService {

    public static final int TOKEN_MINUTES = 30;

    private final UserDao userDao = new UserDao();
    private final PasswordResetDao resetDao = new PasswordResetDao();
    private final AuditLogDao auditDao = new AuditLogDao();
    private final MailService mailService = new MailService();

    /** Không báo lỗi khi email không tồn tại: người gọi luôn hiển thị cùng một thông báo. */
    public void requestReset(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            throw new ServiceException("Vui lòng nhập email.");
        }
        String token = Tokens.random();
        User user = Db.inTransaction(conn -> {
            User u = userDao.findByEmail(conn, email.trim());
            if (u == null || u.getStatus() == User.Status.LOCKED) {
                return null;
            }
            resetDao.invalidateOpenTokens(conn, u.getId());
            resetDao.insert(conn, u.getId(), Tokens.sha256(token), LocalDateTime.now().plusMinutes(TOKEN_MINUTES));
            return u;
        });
        if (user != null) {
            String link = AppConfig.get("app.baseUrl") + "/reset-password?token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8);
            mailService.send(user.getEmail(), "Đặt lại mật khẩu OMS",
                    "Chào " + user.getFullName() + ",\nMở liên kết sau trong " + TOKEN_MINUTES
                            + " phút để đặt mật khẩu mới (chỉ dùng được một lần):\n" + link);
        }
    }

    public boolean isTokenValid(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return false;
        }
        try (var conn = Db.getConnection()) {
            return resetDao.findValidUserId(conn, Tokens.sha256(token)) != null;
        }
    }

    public void resetPassword(String token, String newPassword, String confirmPassword) throws SQLException {
        String strengthError = Passwords.validateStrength(newPassword);
        if (strengthError != null) {
            throw new ServiceException(strengthError);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ServiceException("Mật khẩu nhập lại không khớp.");
        }
        String tokenHash = Tokens.sha256(token == null ? "" : token);
        Db.inTransaction(conn -> {
            Long userId = resetDao.findValidUserId(conn, tokenHash);
            if (userId == null || !resetDao.markUsed(conn, tokenHash)) {
                throw new InvalidTokenException();
            }
            userDao.updatePassword(conn, userId, Passwords.hash(newPassword));
            auditDao.log(conn, userId, "PASSWORD_RESET", "USER", userId, null);
            return null;
        });
    }

    public static class InvalidTokenException extends ServiceException {
        public InvalidTokenException() {
            super("Liên kết đặt lại mật khẩu đã hết hạn hoặc đã được sử dụng.");
        }
    }
}
