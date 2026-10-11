package com.oms.web;

import java.lang.reflect.RecordComponent;
import java.util.Map;

/** Chỉ tuần tự hoá DTO/record được endpoint chọn, không tự đọc bean hoặc thông tin người dùng. */
public final class Json {
    private Json() { }
    public static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?,?> map) {
            var out = new java.util.StringJoiner(",", "{", "}");
            map.forEach((k,v) -> out.add(encode(k.toString()) + ":" + encode(v)));
            return out.toString();
        }
        if (value instanceof Iterable<?> list) {
            var out = new java.util.StringJoiner(",", "[", "]");
            for (Object item : list) out.add(encode(item));
            return out.toString();
        }
        if (value.getClass().isRecord()) {
            var out = new java.util.StringJoiner(",", "{", "}");
            try {
                for (RecordComponent field : value.getClass().getRecordComponents()) {
                    out.add(encode(field.getName()) + ":" + encode(field.getAccessor().invoke(value)));
                }
            } catch (ReflectiveOperationException e) { throw new IllegalArgumentException("DTO không hợp lệ", e); }
            return out.toString();
        }
        String text = value.toString();
        var out = new StringBuilder("\"");
        for (int i=0; i<text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
