package com.oms.web.servlet;
import com.oms.service.*;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet({"/api/orders/purchase-history","/api/orders/add-last-purchase"})
public class PurchaseHistoryServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,false); }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,true); }
    private void handle(HttpServletRequest req,HttpServletResponse resp,boolean post) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        if(post!=req.getServletPath().endsWith("add-last-purchase")) { resp.setStatus(405); return; }
        try {
            var service=new PurchaseHistoryService();
            if(post) { service.addLastPurchase(longParam(req,"id"),currentUser(req)); resp.getWriter().write("{\"ok\":true}"); }
            else resp.getWriter().write(Json.encode(service.history(longParam(req,"customerId"),currentUser(req))));
        } catch(OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(SQLException e) { log("Không xử lý được lịch sử mua hàng",e); resp.setStatus(500); resp.getWriter().write("{\"error\":\"Không thể xử lý lịch sử mua hàng.\"}"); }
    }
}
