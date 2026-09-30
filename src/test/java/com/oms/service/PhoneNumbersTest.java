package com.oms.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNumbersTest {

    @Test
    void acceptsVietnameseMobileAndLandlineInCommonFormats() {
        assertEquals("0912345678", PhoneNumbers.normalize("0912 345 678"));
        assertEquals("0912345678", PhoneNumbers.normalize("0912.345.678"));
        assertEquals("0912345678", PhoneNumbers.normalize("+84 912 345 678"));
        assertEquals("0912345678", PhoneNumbers.normalize("84912345678"));
        assertEquals("0356789012", PhoneNumbers.normalize("035-678-9012"));
        assertEquals("02438123456", PhoneNumbers.normalize("(024) 3812 3456"));
    }

    @Test
    void blankMeansNoPhone() {
        assertNull(PhoneNumbers.normalize(""));
        assertNull(PhoneNumbers.normalize("   "));
        assertNull(PhoneNumbers.normalize(null));
    }

    @Test
    void rejectsWrongLengthPrefixOrLetters() {
        assertThrows(ServiceException.class, () -> PhoneNumbers.normalize("091234567"));    // thiếu số
        assertThrows(ServiceException.class, () -> PhoneNumbers.normalize("09123456789"));  // thừa số
        assertThrows(ServiceException.class, () -> PhoneNumbers.normalize("0112345678"));   // đầu số không tồn tại
        assertThrows(ServiceException.class, () -> PhoneNumbers.normalize("0912abc678"));
        assertThrows(ServiceException.class, () -> PhoneNumbers.normalize("+1 202 555 0100"));
    }
}
