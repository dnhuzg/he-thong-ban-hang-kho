package com.oms.web.servlet;

import com.oms.model.NamedItem;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import com.oms.service.StockLedgerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

/** GHTK-53: sổ tồn theo SKU và kho; GHTK-83: xuất Excel theo bộ lọc đang áp dụng; GHTK-57: tồn tối thiểu. */
@WebServlet({"/inventory/stock", "/inventory/stock/export", "/inventory/stock/minimum"})
public class StockPageServlet extends BaseServlet {

    private final StockLedgerService stockService = new StockLedgerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long warehouse = longParam(req, "warehouse");
        long category = longParam(req, "category");
        StockLedgerService.Filter filter = new StockLedgerService.Filter(param(req, "q"),
                warehouse > 0 ? warehouse : null, category > 0 ? category : null, param(req, "status"));
        try {
            if ("/inventory/stock/export".equals(req.getServletPath())) {
                StockLedgerService.Export file = stockService.exportXlsx(filter);
                resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                        + URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8));
                resp.setContentLength(file.content().length);
                resp.getOutputStream().write(file.content());
                return;
            }
            StockLedgerService.Page page = stockService.page(filter);
            req.setAttribute("stock", page);
            req.setAttribute("minimumWarehouses", page.warehouses().stream().map(NamedItem::getId)
                    .filter(id -> StockLedgerService.canSetMinimum(currentUser(req), id)).collect(Collectors.toSet()));
            render(req, resp, "inventory/stock");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            stockService.setMinimum(longParam(req, "warehouseId"), longParam(req, "productId"), param(req, "minQuantity"),
                    currentUser(req));
            flash(req, "Đã lưu tồn tối thiểu.");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        // Quay lại đúng bộ lọc đang xem
        String back = param(req, "back");
        redirect(req, resp, "/inventory/stock" + (back.startsWith("?") ? back : ""));
    }
}
