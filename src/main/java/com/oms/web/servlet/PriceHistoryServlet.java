package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.service.PermissionService;
import com.oms.service.PriceHistoryService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Lịch sử thay đổi giá của một sản phẩm (S3-02); chỉ đọc. */
@WebServlet("/products/price-history")
public class PriceHistoryServlet extends BaseServlet {

    private final PriceHistoryService historyService = new PriceHistoryService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            // giống bảng giá: người chỉ xem phạm vi của mình không xem được giá
            if (permissionService.policy().isOwnScopeOnly(currentUser(req).getRoles(), Module.PRODUCT)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            long productId = longParam(req, "id");
            req.setAttribute("product", historyService.product(productId));
            req.setAttribute("changes", historyService.history(productId));
            render(req, resp, "products/price-history");
        } catch (PriceHistoryService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
