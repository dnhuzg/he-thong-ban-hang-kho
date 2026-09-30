package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.UserDao;
import com.oms.model.User;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.SQLException;

/** Ảnh đại diện (S2-03): JPG/PNG tối đa 2MB, cắt vuông 256px kèm bản thu nhỏ 64px cho topbar và lịch sử. */
public class AvatarService {

    public static final int SIZE = 256;
    public static final int THUMB_SIZE = 64;
    private static final String FOLDER = "avatars";

    private final UserDao userDao = new UserDao();
    private final AuditLogDao auditDao = new AuditLogDao();
    private final FileStorage storage;

    public AvatarService() {
        this(new FileStorage());
    }

    public AvatarService(FileStorage storage) {
        this.storage = storage;
    }

    public void upload(User actor, byte[] data) throws SQLException, IOException {
        BufferedImage image = Images.readJpegOrPng(data);
        String name = FileStorage.randomName();
        String path = storage.write(FOLDER, name + ".png", Images.squarePng(image, SIZE));
        String thumb = storage.write(FOLDER, name + "-64.png", Images.squarePng(image, THUMB_SIZE));
        String old;
        try {
            old = replaceAvatar(actor, path);
        } catch (SQLException | RuntimeException e) {
            // DB không nhận thì dọn tệp vừa ghi để không sinh rác trong thư mục upload
            storage.deleteQuietly(path);
            storage.deleteQuietly(thumb);
            throw e;
        }
        deleteFiles(old);
    }

    public void remove(User actor) throws SQLException {
        deleteFiles(replaceAvatar(actor, null));
    }

    /** Ghi đường dẫn mới, trả về đường dẫn cũ để xoá tệp sau khi transaction đã commit. */
    private String replaceAvatar(User actor, String newPath) throws SQLException {
        return Db.inTransaction(conn -> {
            User current = userDao.findByIdForUpdate(conn, actor.getId());
            if (current == null || current.getStatus() == User.Status.LOCKED) {
                throw new ServiceException("Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại.");
            }
            userDao.updateAvatar(conn, current.getId(), newPath);
            auditDao.log(conn, current.getId(), newPath == null ? "AVATAR_REMOVE" : "AVATAR_UPDATE",
                    "USER", current.getId(), null);
            return current.getAvatarPath();
        });
    }

    private void deleteFiles(String path) {
        if (path == null) {
            return;
        }
        storage.deleteQuietly(path);
        storage.deleteQuietly(path.replaceFirst("\\.png$", "-64.png"));
    }
}
