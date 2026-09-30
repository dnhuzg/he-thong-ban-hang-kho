package com.oms.service;

import com.oms.config.AppConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Lưu tệp người dùng tải lên (ảnh đại diện, ảnh sản phẩm) vào thư mục upload.dir, nằm ngoài Tomcat
 * để không mất khi deploy lại. DB chỉ lưu đường dẫn tương đối, ví dụ "avatars/3f2c...png".
 */
public class FileStorage {

    private final Path root;

    public FileStorage() {
        this(defaultRoot());
    }

    public FileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    private static Path defaultRoot() {
        String dir = AppConfig.get("upload.dir");
        return dir.isEmpty() ? Paths.get(System.getProperty("user.home"), "oms-uploads") : Paths.get(dir);
    }

    /** Ghi dữ liệu vào thư mục con với tên cho trước; trả về đường dẫn tương đối. */
    public String write(String folder, String fileName, byte[] data) throws IOException {
        Path dir = resolve(folder);
        Files.createDirectories(dir);
        Files.write(dir.resolve(fileName), data);
        return folder + "/" + fileName;
    }

    public static String randomName() {
        return UUID.randomUUID().toString();
    }

    /** Chặn đường dẫn kiểu "../" thoát khỏi thư mục upload. */
    public Path resolve(String relative) {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ: " + relative);
        }
        return path;
    }

    public void deleteQuietly(String relative) {
        if (relative == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relative));
        } catch (IOException | IllegalArgumentException ignored) {
            // Tệp cũ còn sót lại không ảnh hưởng dữ liệu; không chặn thao tác của người dùng
        }
    }
}
