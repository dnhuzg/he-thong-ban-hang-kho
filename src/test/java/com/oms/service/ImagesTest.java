package com.oms.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImagesTest {

    // 2x1 WebP image generated as a valid WebP fixture.
    private static final byte[] WEBP = Base64.getDecoder().decode(
            "UklGRjYAAABXRUJQVlA4ICoAAADwAQCdASoCAAEAAUAmJaACdLoB+AAEgwAA/upl//yC5YXXI1/bsFnLGAA=");

    @Test
    void rejectsWebpBecauseReceiptUploadOnlyAcceptsJpgOrPng() {
        assertThrows(ServiceException.class, () -> Images.readJpegOrPng(WEBP));
    }

    @Test
    void receiptConversionPreservesAspectRatioInsteadOfCropping() throws Exception {
        BufferedImage source = new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB);
        byte[] png = Images.fitPng(source, 600);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(600, result.getWidth());
        assertEquals(300, result.getHeight());
    }

    @Test
    void rejectsNonImageUpload() {
        assertThrows(ServiceException.class, () -> Images.readJpegOrPng("not an image".getBytes()));
    }
}
