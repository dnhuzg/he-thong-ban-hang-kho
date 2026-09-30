package com.oms.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordsTest {

    @Test
    void strengthRules() {
        assertNotNull(Passwords.validateStrength("abc123"));      // quá ngắn
        assertNotNull(Passwords.validateStrength("abcdefgh"));    // thiếu số
        assertNotNull(Passwords.validateStrength("12345678"));    // thiếu chữ
        assertNull(Passwords.validateStrength("matkhau2026"));
    }

    @Test
    void bcryptLimitCountsUtf8BytesInsteadOfCharacters() {
        assertNull(Passwords.validateStrength("a".repeat(71) + "1"));
        assertNotNull(Passwords.validateStrength("a".repeat(72) + "1"));
        assertNotNull(Passwords.validateStrength("ế".repeat(24) + "1"));
    }

    @Test
    void temporaryPasswordAlwaysValid() {
        for (int i = 0; i < 50; i++) {
            assertNull(Passwords.validateStrength(Passwords.generateTemporary()));
        }
    }

    @Test
    void hashIsNotPlainTextAndVerifies() {
        String hash = Passwords.hash("matkhau2026");
        assertFalse(hash.contains("matkhau2026"));
        assertTrue(Passwords.matches("matkhau2026", hash));
        assertFalse(Passwords.matches("sai-mat-khau1", hash));
    }
}
