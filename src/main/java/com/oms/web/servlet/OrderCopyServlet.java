package com.oms.web.servlet;
import com.oms.service.*;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/orders/copy")
public class OrderCopyServlet extends BaseServlet {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try { long id=new OrderCopyService().copy(longParam(req,"id"),currentUser(req)); resp.setStatus(201); resp.getWriter().write(Json.encode(Map.of("id",id,"status","DRAFT"))); }
        catch(OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(SQLException e) { log("Không sao chép được đơn",e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể sao chép đơn."))); }
    }
}
