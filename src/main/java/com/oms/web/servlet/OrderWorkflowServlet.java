package com.oms.web.servlet;
import com.oms.service.*;
import com.oms.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet({"/api/orders/submit","/api/orders/pending","/api/orders/decide","/api/orders/history"})
public class OrderWorkflowServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,false); }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,true); }
    private void handle(HttpServletRequest req,HttpServletResponse resp,boolean post) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path=req.getServletPath();
        boolean mutation=path.endsWith("submit") || path.endsWith("decide");
        if(post!=mutation) { resp.setStatus(405); return; }
        try {
            var service=new OrderWorkflowService();
            Object result;
            if(path.endsWith("submit")) result=Map.of("status",service.submit(longParam(req,"id"),currentUser(req)));
            else if(path.endsWith("decide")) { service.decide(longParam(req,"id"),param(req,"action"),param(req,"opinion"),currentUser(req)); result=Map.of("ok",true); }
            else if(path.endsWith("pending")) result=service.pending(currentUser(req));
            else result=service.history(longParam(req,"id"),currentUser(req));
            resp.getWriter().write(Json.encode(result));
        } catch(OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(SQLException e) { log("Không xử lý được duyệt đơn",e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể xử lý đơn. Vui lòng thử lại."))); }
    }
}
