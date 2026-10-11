package com.oms.web.servlet;
import com.oms.service.OrderCreditService;
import com.oms.service.OrderService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/orders/credit")
public class OrderCreditServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try { resp.getWriter().write(Json.encode(new OrderCreditService().quote(longParam(req,"id"),currentUser(req)))); }
        catch (OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch (ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch (ServiceException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch (SQLException e) { log("Không đọc được công nợ đơn", e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể đọc công nợ. Vui lòng thử lại."))); }
    }
}
