package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.UserDao;
import com.oms.model.User;
import com.oms.security.Passwords;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    // Câu chung cho mọi trường hợp sai: không tiết lộ tài khoản có tồn tại hay đang bị tạm khoá (S1-01)
    public static final String GENERIC_LOGIN_ERROR =
            "Tài khoản hoặc mật khẩu không đúng. Nhập sai " + MAX_FAILED_ATTEMPTS
                    + " lần liên tiếp, tài khoản sẽ bị tạm khoá " + LOCK_MINUTES + " phút.";

    // Hash giả để thời gian phản hồi khi tài khoản không tồn tại gần bằng khi sai mật khẩu
    private static final String DUMMY_HASH = Passwords.hash("dummy-password-1");

    private final UserDao userDao = new UserDao();
    private final AuditLogDao auditDao = new AuditLogDao();

    public User login(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tài khoản và mật khẩu.");
        }
        // Trả kết quả lỗi ra ngoài transaction để bộ đếm đăng nhập sai vẫn được commit.
        LoginResult result = Db.inTransaction(conn -> {
            User user = userDao.findByUsernameForUpdate(conn, username.trim());
            if (user == null) {
                Passwords.matches(password, DUMMY_HASH);
                return new LoginResult(null, GENERIC_LOGIN_ERROR);
            }

            LocalDateTime now = LocalDateTime.now();
            if (user.getLoginLockedUntil() != null && user.getLoginLockedUntil().isAfter(now)) {
                return new LoginResult(null, GENERIC_LOGIN_ERROR);
            }

            if (!Passwords.matches(password, user.getPasswordHash())) {
                int failed = user.getFailedLoginCount() + 1;
                userDao.recordLoginFailure(conn, user.getId(),
                        failed >= MAX_FAILED_ATTEMPTS ? 0 : failed,
                        failed >= MAX_FAILED_ATTEMPTS ? now.plusMinutes(LOCK_MINUTES) : null);
                return new LoginResult(null, GENERIC_LOGIN_ERROR);
            }

            if (user.getStatus() == User.Status.LOCKED) {
                return new LoginResult(null, "Tài khoản của bạn đã bị khoá. Vui lòng liên hệ quản trị hệ thống.");
            }

            userDao.recordLoginSuccess(conn, user.getId());
            auditDao.log(conn, user.getId(), "LOGIN", "USER", user.getId(), null);
            return new LoginResult(user, null);
        });
        if (result.error() != null) {
            throw new ServiceException(result.error());
        }
        return result.user();
    }

    private record LoginResult(User user, String error) {
    }

    public User findActiveSessionUser(long userId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return userDao.findById(conn, userId);
        }
    }

    /** Đổi mật khẩu khi đang đăng nhập (S1-04). Trả về session_version mới để giữ phiên hiện tại. */
    public int changePassword(long userId, String currentPassword, String newPassword, String confirmPassword)
            throws SQLException {
        String strengthError = Passwords.validateStrength(newPassword);
        if (strengthError != null) {
            throw new ServiceException(strengthError);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ServiceException("Mật khẩu nhập lại không khớp.");
        }
        return Db.inTransaction(conn -> {
            User user = userDao.findByIdForUpdate(conn, userId);
            if (user == null || user.getStatus() == User.Status.LOCKED
                    || !Passwords.matches(currentPassword, user.getPasswordHash())) {
                throw new ServiceException("Mật khẩu hiện tại không đúng.");
            }
            if (Passwords.matches(newPassword, user.getPasswordHash())) {
                throw new ServiceException("Mật khẩu mới phải khác mật khẩu hiện tại.");
            }
            int version = userDao.updatePassword(conn, userId, Passwords.hash(newPassword));
            auditDao.log(conn, userId, "PASSWORD_CHANGE", "USER", userId, null);
            return version;
        });
    }
}
