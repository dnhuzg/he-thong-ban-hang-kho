package com.oms.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Đọc app.properties trên classpath (file này không commit, xem app.properties.example). */
public final class AppConfig {

    private static final Properties PROPS = load();

    private AppConfig() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in == null) {
                throw new IllegalStateException("Thiếu app.properties — chép từ app.properties.example rồi điền giá trị.");
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được app.properties", e);
        }
        return props;
    }

    public static String get(String key) {
        String value = PROPS.getProperty(key);
        return value == null ? "" : value.trim();
    }
}
