package com.oms.service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/** Kiểm tra và xử lý ảnh tải lên dựa trên định dạng ảnh thực tế. */
public final class Images {

    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private Images() {
    }

    /** Chỉ nhận ảnh JPG/PNG; các màn hình hiện hữu dùng phương thức này vẫn giữ nguyên hợp đồng. */
    public static BufferedImage readJpegOrPng(byte[] data) {
        return readSupportedImage(data, Set.of("jpeg", "png"), "JPG hoặc PNG");
    }

    private static BufferedImage readSupportedImage(byte[] data, Set<String> allowedFormats, String label) {
        if (data == null || data.length == 0) {
            throw new ServiceException("Vui lòng chọn một tệp ảnh.");
        }
        if (data.length > MAX_BYTES) {
            throw new ServiceException("Ảnh tối đa 2MB.");
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (in == null) {
                throw new ServiceException("Tệp không phải ảnh " + label + " hợp lệ.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            while (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!allowedFormats.contains(format)) {
                        continue;
                    }
                    reader.setInput(in);
                    BufferedImage image = reader.read(0);
                    if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                        throw new ServiceException("Tệp không phải ảnh " + label + " hợp lệ.");
                    }
                    return image;
                } finally {
                    reader.dispose();
                }
            }
            throw new ServiceException("Chỉ chấp nhận ảnh " + label + ".");
        } catch (IOException e) {
            throw new ServiceException("Tệp không phải ảnh " + label + " hợp lệ.");
        }
    }

    /** Cắt hình vuông ở giữa rồi thu về size × size, xuất PNG. Dùng cho avatar/ảnh sản phẩm. */
    public static byte[] squarePng(BufferedImage source, int size) {
        if (source == null || size < 1) {
            throw new IllegalArgumentException("Ảnh và kích thước phải hợp lệ.");
        }
        int side = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - side) / 2;
        int y = (source.getHeight() - side) / 2;
        BufferedImage square = source.getSubimage(x, y, side, side);

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(square, 0, 0, size, size, null);
        g.dispose();
        return pngBytes(out);
    }

    /** Thu nhỏ tối đa maxSide mà giữ nguyên tỷ lệ, không cắt mất mép ảnh biên lai/chứng từ. */
    public static byte[] fitPng(BufferedImage source, int maxSide) {
        if (source == null || source.getWidth() < 1 || source.getHeight() < 1 || maxSide < 1) {
            throw new IllegalArgumentException("Ảnh và kích thước phải hợp lệ.");
        }
        double scale = Math.min(1.0, (double) maxSide / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return pngBytes(out);
    }

    private static byte[] pngBytes(BufferedImage image) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            if (!ImageIO.write(image, "png", bytes)) {
                throw new IllegalStateException("Không tìm thấy bộ mã hóa PNG.");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không ghi được ảnh PNG", e);
        }
        return bytes.toByteArray();
    }
}
