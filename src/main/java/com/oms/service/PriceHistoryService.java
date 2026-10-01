package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.PriceHistoryDao;
import com.oms.dao.ProductDao;
import com.oms.model.PriceChange;
import com.oms.model.Product;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Lịch sử thay đổi giá của một sản phẩm (S3-02). Việc ghi lịch sử nằm trong PriceListService, cùng giao dịch với việc đổi giá. */
public class PriceHistoryService {

    private final PriceHistoryDao historyDao = new PriceHistoryDao();
    private final ProductDao productDao = new ProductDao();

    /** Sản phẩm (không kèm giá vốn) cho tiêu đề trang; ném NotFoundException nếu không có. */
    public Product product(long productId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            Product p = productDao.findById(conn, productId, false);
            if (p == null) {
                throw new NotFoundException();
            }
            return p;
        }
    }

    public List<PriceChange> history(long productId) throws SQLException {
        try (Connection conn = Db.getConnection()) {
            return historyDao.listByProduct(conn, productId);
        }
    }

    public static class NotFoundException extends ServiceException {
        public NotFoundException() {
            super("Không tìm thấy sản phẩm.");
        }
    }
}
