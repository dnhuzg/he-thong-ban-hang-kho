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

/** Kiểm tra và xử lý ảnh tải lên bằng ImageIO có sẵn của Java (không cần thư viện ngoài). */
public final class Images {

    public static final long MAX_BYTES = 2L * 1024 * 1024;

    private Images() {
    }

    /**
     * Chỉ nhận JPG/PNG thật: xác định định dạng từ nội dung tệp chứ không tin đuôi tên tệp
     * hay Content-Type trình duyệt gửi lên.
     */
    public static BufferedImage readJpegOrPng(byte[] data) {
        if (data == null || data.length == 0) {
            throw new ServiceException("Vui lòng chọn một tệp ảnh.");
        }
        if (data.length > MAX_BYTES) {
            throw new ServiceException("Ảnh tối đa 2MB.");
        }
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new ServiceException("Tệp không phải ảnh JPG hoặc PNG hợp lệ.");
            }
            ImageReader reader = readers.next();
            String format = reader.getFormatName().toLowerCase(Locale.ROOT);
            if (!format.equals("jpeg") && !format.equals("png")) {
                throw new ServiceException("Chỉ chấp nhận ảnh JPG hoặc PNG.");
            }
            reader.setInput(in);
            BufferedImage image = reader.read(0);
            reader.dispose();
            return image;
        } catch (IOException e) {
            throw new ServiceException("Tệp không phải ảnh JPG hoặc PNG hợp lệ.");
        }
    }

    /** Cắt hình vuông ở giữa rồi thu về size × size, xuất PNG. */
    public static byte[] squarePng(BufferedImage source, int size) {
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

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            ImageIO.write(out, "png", bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Không ghi được ảnh PNG", e);
        }
        return bytes.toByteArray();
    }
}
