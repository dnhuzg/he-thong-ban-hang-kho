package com.oms.web.servlet;

import com.oms.service.ForbiddenException;
import com.oms.service.InventoryLotService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** S6-01: tra cứu lô thật, cấu hình SKU và bổ sung thông tin lô cũ. */
@WebServlet({"/inventory/lots", "/inventory/lots/configure", "/inventory/lots/metadata"})
public class InventoryLotsPageServlet extends BaseServlet {
    private final InventoryLotService service = new InventoryLotService();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/inventory/lots".equals(req.getServletPath())) { resp.sendError(405); return; }
        long product=longParam(req,"product"), warehouse=longParam(req,"warehouse");
        try {
            req.setAttribute("lotPage",service.page(product>0?product:null,warehouse>0?warehouse:null,param(req,"status"),param(req,"q"),currentUser(req)));
            render(req,resp,"inventory/lots");
        } catch (ForbiddenException e) { resp.sendError(403); }
        catch (ServiceException e) { flash(req,e.getMessage()); redirect(req,resp,"/inventory/lots"); }
        catch (SQLException e) { throw new ServletException(e); }
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long product=longParam(req,"product");
        String back="/inventory/lots"+(product>0?"?product="+product:"");
        try {
            switch (req.getServletPath()) {
                case "/inventory/lots/configure" -> {
                    int days;
                    try { days=Integer.parseInt(param(req,"warningDays")); }
                    catch (NumberFormatException e) { throw new ServiceException("Ngưỡng cảnh báo phải là số nguyên từ 0 đến 365."); }
                    service.configure(product,"true".equals(param(req,"tracked")),days,currentUser(req));
                    flash(req,"Đã lưu cấu hình quản lý lô vào cơ sở dữ liệu.");
                }
                case "/inventory/lots/metadata" -> {
                    service.updateMetadata(longParam(req,"id"),param(req,"lotNo"),date(param(req,"manufacturedDate")),date(param(req,"expiryDate")),currentUser(req));
                    flash(req,"Đã bổ sung thông tin lô. Số tồn không thay đổi.");
                }
                default -> { resp.sendError(404); return; }
            }
            redirect(req,resp,back);
        } catch (ForbiddenException e) { resp.sendError(403); }
        catch (ServiceException e) { flash(req,e.getMessage()); redirect(req,resp,back); }
        catch (SQLException e) { throw new ServletException(e); }
    }
    private static LocalDate date(String value) {
        if (value.isEmpty()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { throw new ServiceException("Ngày sản xuất hoặc hạn dùng không hợp lệ."); }
    }
}
