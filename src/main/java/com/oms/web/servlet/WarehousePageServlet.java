package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.service.PermissionService;
import com.oms.service.ServiceException;
import com.oms.service.WarehouseAdminService;
import com.oms.web.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/** GHTK-51: khai báo kho, vị trí và kho phục vụ đại lý trên MySQL. */
@WebServlet({"/inventory/warehouses","/inventory/warehouses/save","/inventory/locations/save","/inventory/warehouses/assign"})
public class WarehousePageServlet extends BaseServlet {
    private final PermissionService permissions=new PermissionService();private final WarehouseAdminService service=new WarehouseAdminService();
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{if(!"/inventory/warehouses".equals(req.getServletPath())){resp.sendError(405);return;}try{req.setAttribute("canWrite",permissions.policy().canWrite(currentUser(req).getRoles(),Module.INVENTORY));req.setAttribute("warehouseData",Json.encode(service.page()).replace("<","\\u003c"));render(req,resp,"inventory/warehouses");}catch(SQLException e){throw new ServletException(e);}}
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{resp.setContentType("application/json;charset=UTF-8");try{long id;switch(req.getServletPath()){
        case "/inventory/warehouses/save" -> id=service.saveWarehouse(nullableId(req,"id"),param(req,"code"),param(req,"name"),param(req,"address"),param(req,"manager"),param(req,"status"),currentUser(req));
        case "/inventory/locations/save" -> id=service.saveLocation(nullableId(req,"id"),requiredId(req,"warehouseId"),param(req,"code"),param(req,"name"),param(req,"type"),nullableId(req,"parentId"),currentUser(req));
        case "/inventory/warehouses/assign" -> {service.assign(requiredId(req,"dealerId"),requiredId(req,"warehouseId"),currentUser(req));id=requiredId(req,"dealerId");}
        default -> {error(resp,405,"Thao tác không hợp lệ.");return;}}
        resp.getWriter().write(Json.encode(Map.of("id",id)));
    }catch(ServiceException|IllegalArgumentException e){error(resp,400,e.getMessage());}catch(SQLException e){log("Không lưu được dữ liệu kho",e);error(resp,400,"Mã đã tồn tại hoặc dữ liệu đang được tham chiếu.");}}
    private static Long nullableId(HttpServletRequest req,String name){String x=param(req,name);if(x.isBlank())return null;try{long id=Long.parseLong(x);return id>0?id:null;}catch(NumberFormatException e){throw new ServiceException("ID không hợp lệ.");}}
    private static long requiredId(HttpServletRequest req,String name){Long id=nullableId(req,name);if(id==null)throw new ServiceException("Thiếu dữ liệu tham chiếu.");return id;}
    private static void error(HttpServletResponse resp,int status,String message)throws IOException{resp.setStatus(status);resp.getWriter().write(Json.encode(Map.of("error",message)));}
}
