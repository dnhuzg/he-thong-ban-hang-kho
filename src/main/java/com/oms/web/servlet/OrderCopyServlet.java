package com.oms.web.servlet;
import com.oms.service.*;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet({"/api/orders/copy", "/orders/copy"})
public class OrderCopyServlet extends BaseServlet {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        if ("/orders/copy".equals(req.getServletPath())) {
            copyFromPage(req, resp);
            return;
        }
        resp.setContentType("application/json;charset=UTF-8");
        try { long id=new OrderCopyService().copy(longParam(req,"id"),currentUser(req)); resp.setStatus(201); resp.getWriter().write(Json.encode(Map.of("id",id,"status","DRAFT"))); }
        catch(OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(SQLException e) { log("Không sao chép được đơn",e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể sao chép đơn."))); }
    }

    private void copyFromPage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long sourceId = longParam(req, "id");
        try {
            long id = new OrderCopyService().copy(sourceId, currentUser(req));
            flash(req, "Đã sao chép thành đơn nháp mới. Giá và chiết khấu áp dụng theo chính sách hiện hành; kiểm tra lại ngày và điểm giao trước khi chốt.");
            redirect(req, resp, "/orders/edit?id=" + id);
        } catch (OrderService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ServiceException e) {
            flash(req, "Không thể sao chép: " + e.getMessage());
            redirect(req, resp, "/orders/edit?id=" + sourceId);
        } catch (SQLException e) {
            log("Không sao chép được đơn", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
