package com.oms.web.servlet;
import com.oms.dao.OrderSearchDao;
import com.oms.model.Order;
import com.oms.service.*;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

@WebServlet("/api/orders/search")
public class OrderSearchServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            String status=param(req,"status");
            var filter=new OrderSearchDao.Filter(status.isEmpty()?null:Order.Status.valueOf(status),number(req,"customerId"),number(req,"creatorId"),number(req,"areaId"),date(req,"from"),date(req,"to"));
            resp.getWriter().write(Json.encode(new OrderSearchService().search(filter,intParam(req,"page",1),intParam(req,"pageSize",25),currentUser(req))));
        } catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException | IllegalArgumentException | DateTimeParseException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error","Bộ lọc không hợp lệ."))); }
        catch(SQLException e) { log("Không tìm được đơn",e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể đọc danh sách đơn."))); }
    }
    private Long number(HttpServletRequest req,String key) { String v=param(req,key); if(v.isEmpty()) return null; long n=Long.parseLong(v); if(n<1) throw new IllegalArgumentException(); return n; }
    private LocalDate date(HttpServletRequest req,String key) { String v=param(req,key); return v.isEmpty()?null:LocalDate.parse(v); }
}
