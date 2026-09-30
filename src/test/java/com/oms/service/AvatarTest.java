package com.oms.service;

import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S2-03: chỉ JPG/PNG ≤ 2MB, cắt vuông, có bản thu nhỏ; ảnh cũ bị dọn khi thay. */
class AvatarTest extends DatabaseTest {

    @TempDir
    Path uploads;

    @Test
    void cropsToSquareAndCreatesThumbnail() throws Exception {
        User rep = user("rep01", Role.SALES_REP);
        new AvatarService(new FileStorage(uploads)).upload(rep, image("jpg", 400, 300));

        String path = reload(rep.getId()).getAvatarPath();
        assertTrue(path.startsWith("avatars/") && path.endsWith(".png"));
        BufferedImage large = ImageIO.read(uploads.resolve(path).toFile());
        BufferedImage thumb = ImageIO.read(uploads.resolve(path.replace(".png", "-64.png")).toFile());
        assertEquals(256, large.getWidth());
        assertEquals(256, large.getHeight());
        assertEquals(64, thumb.getWidth());
        assertEquals(64, thumb.getHeight());
    }

    @Test
    void replacingDeletesOldFilesAndRemoveClearsPath() throws Exception {
        User rep = user("rep02", Role.SALES_REP);
        AvatarService service = new AvatarService(new FileStorage(uploads));
        service.upload(rep, image("png", 100, 100));
        String first = reload(rep.getId()).getAvatarPath();

        service.upload(rep, image("png", 120, 80));
        assertFalse(Files.exists(uploads.resolve(first)));
        assertFalse(Files.exists(uploads.resolve(first.replace(".png", "-64.png"))));

        service.remove(rep);
        assertNull(reload(rep.getId()).getAvatarPath());
        try (var files = Files.list(uploads.resolve("avatars"))) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void rejectsNonImageOtherFormatsAndTooLarge() {
        assertThrows(ServiceException.class, () -> Images.readJpegOrPng("không phải ảnh".getBytes()));
        assertThrows(ServiceException.class, () -> Images.readJpegOrPng(image("bmp", 10, 10)));
        assertThrows(ServiceException.class, () -> Images.readJpegOrPng(new byte[(int) Images.MAX_BYTES + 1]));
        assertThrows(ServiceException.class, () -> Images.readJpegOrPng(new byte[0]));
    }

    @Test
    void storageBlocksPathTraversal() {
        FileStorage storage = new FileStorage(uploads);
        assertThrows(IllegalArgumentException.class, () -> storage.resolve("../app.properties"));
        assertThrows(IllegalArgumentException.class, () -> storage.resolve("avatars/../../x.png"));
    }

    @Test
    void lockedUserCannotUploadAndNoFileIsLeftBehind() throws Exception {
        User rep = user("rep03", Role.SALES_REP);
        sql("UPDATE users SET status = 'LOCKED' WHERE id = " + rep.getId());
        assertThrows(ServiceException.class,
                () -> new AvatarService(new FileStorage(uploads)).upload(rep, image("png", 50, 50)));
        try (var files = Files.list(uploads.resolve("avatars"))) {
            assertEquals(0, files.count());
        }
    }

    private static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, format, out);
        byte[] bytes = out.toByteArray();
        // Đảm bảo ảnh test đọc lại được, tránh test "đạt" vì ảnh hỏng
        assertTrue(ImageIO.read(new ByteArrayInputStream(bytes)) != null);
        return bytes;
    }
}
