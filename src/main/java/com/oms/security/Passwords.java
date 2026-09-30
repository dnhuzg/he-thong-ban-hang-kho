package com.oms.security;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

public final class Passwords {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TEMP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private Passwords() {
    }

    public static String hash(String raw) {
        return BCrypt.hashpw(raw, BCrypt.gensalt(12));
    }

    public static boolean matches(String raw, String hash) {
        if (raw == null || hash == null) {
            return false;
        }
        return BCrypt.checkpw(raw, hash);
    }

    /** Tối thiểu 8 ký tự, có cả chữ và số (S1-04). Trả về null nếu hợp lệ, ngược lại là thông báo lỗi. */
    public static String validateStrength(String raw) {
        if (raw == null || raw.length() < 8) {
            return "Mật khẩu mới phải có tối thiểu 8 ký tự.";
        }
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            return "Mật khẩu mới phải có cả chữ và số.";
        }
        return null;
    }

    /** Mật khẩu tạm cấp khi tạo tài khoản; luôn có cả chữ và số. */
    public static String generateTemporary() {
        StringBuilder sb = new StringBuilder();
        while (validateStrength(sb.toString()) != null) {
            sb.setLength(0);
            for (int i = 0; i < 10; i++) {
                sb.append(TEMP_CHARS.charAt(RANDOM.nextInt(TEMP_CHARS.length())));
            }
        }
        return sb.toString();
    }
}
