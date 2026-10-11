package com.oms.web.servlet;
import com.oms.model.Order;
import com.oms.service.*;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet({"/api/orders/cancel","/api/orders/advance"})
public class OrderLifecycleServlet extends BaseServlet {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            var service=new OrderLifecycleService();
            if(req.getServletPath().endsWith("cancel")) service.cancel(longParam(req,"id"),param(req,"reason"),currentUser(req));
            else service.advance(longParam(req,"id"),Order.Status.valueOf(param(req,"status")),currentUser(req));
            resp.getWriter().write("{\"ok\":true}");
        } catch(OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException | IllegalArgumentException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error","Bước chuyển hoặc lý do không hợp lệ: "+e.getMessage()))); }
        catch(SQLException e) { log("Không chuyển được vòng đời đơn",e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể chuyển trạng thái. Vui lòng thử lại."))); }
    }
}
