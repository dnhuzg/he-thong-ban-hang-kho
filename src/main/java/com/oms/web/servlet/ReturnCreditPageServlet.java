package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.service.ForbiddenException;
import com.oms.service.ReturnCreditService;
import com.oms.service.ServiceException;
import com.oms.web.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/** GHTK-77: giảm công nợ từ phiếu trả đã nhận. */
@WebServlet({"/finance/return-credits", "/finance/return-credits/create"})
public class ReturnCreditPageServlet extends BaseServlet {
    private final ReturnCreditService service = new ReturnCreditService();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        if(!"/finance/return-credits".equals(req.getServletPath())){resp.sendError(405);return;}
        if(!(currentUser(req).hasRole(Role.ACCOUNTANT)||currentUser(req).isAdmin())){resp.sendError(403);return;}
        try{req.setAttribute("canCreateReturnCredit",true);req.setAttribute("returnCreditData",Json.encode(service.page()).replace("<","\\u003c"));render(req,resp,"finance/return-credits");}
        catch(SQLException e){throw new ServletException(e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        resp.setContentType("application/json;charset=UTF-8");
        if(!"/finance/return-credits/create".equals(req.getServletPath())){error(resp,405,"Thao tác không hợp lệ.");return;}
        try{resp.getWriter().write(Json.encode(service.create(longParam(req,"returnId"),currentUser(req))));}
        catch(ForbiddenException e){error(resp,403,e.getMessage());}catch(ServiceException|IllegalArgumentException e){error(resp,400,e.getMessage());}
        catch(SQLException e){log("Không lập được chứng từ giảm trừ",e);error(resp,500,"Không thể lập chứng từ. Vui lòng thử lại.");}
    }
    private static void error(HttpServletResponse resp,int status,String message)throws IOException{resp.setStatus(status);resp.getWriter().write(Json.encode(Map.of("error",message)));}
}
