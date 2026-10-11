package com.oms.web.servlet;
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

@WebServlet({"/api/portal/catalog","/api/portal/cart","/api/portal/create","/api/portal/add","/api/portal/update","/api/portal/remove","/api/portal/header","/api/portal/submit"})
public class PortalOrderServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,false); }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException { handle(req,resp,true); }
    private void handle(HttpServletRequest req,HttpServletResponse resp,boolean post) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path=req.getServletPath(); boolean read=path.endsWith("catalog") || path.endsWith("cart");
        if(post==read) { resp.setStatus(405); return; }
        var service=new PortalOrderService(); long id=longParam(req,"id"); var actor=currentUser(req);
        try {
            Object result=Map.of("ok",true);
            switch(path) {
                case "/api/portal/catalog" -> result=service.catalog(param(req,"q"),intParam(req,"page",1),actor);
                case "/api/portal/cart" -> result=service.cart(id,actor);
                case "/api/portal/create" -> { result=Map.of("id",service.create(actor)); resp.setStatus(201); }
                case "/api/portal/add" -> service.add(id,longParam(req,"productId"),longParam(req,"unitId"),param(req,"quantity"),actor);
                case "/api/portal/update" -> service.update(id,longParam(req,"lineId"),longParam(req,"unitId"),param(req,"quantity"),actor);
                case "/api/portal/remove" -> service.remove(id,longParam(req,"lineId"),actor);
                case "/api/portal/header" -> {
                    long point=longParam(req,"deliveryPointId"); String date=param(req,"deliveryDate");
                    service.header(id,point>0?point:null,date.isEmpty()?null:LocalDate.parse(date),param(req,"note"),actor);
                }
                case "/api/portal/submit" -> result=Map.of("status",service.submit(id,actor));
                default -> { resp.setStatus(404); return; }
            }
            resp.getWriter().write(Json.encode(result));
        } catch(OrderService.NotFoundException e) { resp.setStatus(404); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ForbiddenException e) { resp.setStatus(403); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(ServiceException | DateTimeParseException e) { resp.setStatus(400); resp.getWriter().write(Json.encode(Map.of("error",e.getMessage()))); }
        catch(SQLException e) { log("Không xử lý được giỏ hàng đại lý",e); resp.setStatus(500); resp.getWriter().write(Json.encode(Map.of("error","Không thể xử lý giỏ hàng. Vui lòng thử lại."))); }
    }
}
