package com.oms.service;

import java.util.regex.Pattern;

/**
 * Số điện thoại Việt Nam (S2-02): di động 10 số đầu 03/05/07/08/09, cố định 11 số đầu 02.
 * Chấp nhận dấu cách, chấm, gạch và tiền tố +84 / 84; lưu về dạng 0xxxxxxxxx để tìm kiếm thống nhất.
 */
public final class PhoneNumbers {

    private static final Pattern MOBILE = Pattern.compile("^0[35789]\\d{8}$");
    private static final Pattern LANDLINE = Pattern.compile("^02\\d{9}$");

    public static final String FORMAT_HINT =
            "Số di động 10 số (bắt đầu 03, 05, 07, 08, 09) hoặc số cố định 11 số (bắt đầu 02).";

    private PhoneNumbers() {
    }

    /** Trả về số đã chuẩn hoá; chuỗi rỗng trả về null; sai định dạng ném ServiceException. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String digits = raw.trim().replaceAll("[\\s.()-]", "");
        if (digits.startsWith("+84")) {
            digits = "0" + digits.substring(3);
        } else if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (!MOBILE.matcher(digits).matches() && !LANDLINE.matcher(digits).matches()) {
            throw new ServiceException("Số điện thoại không hợp lệ. " + FORMAT_HINT);
        }
        return digits;
    }
}
