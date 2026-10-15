package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.security.PermissionPolicy;
import com.oms.service.ForbiddenException;
import com.oms.service.SalesReturnService;
import com.oms.service.ServiceException;
import com.oms.web.Json;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** GHTK-76: phiếu trả hàng dùng dữ liệu thật, tồn chỉ cập nhật khi kho xác nhận. */
@WebServlet({"/sales/returns", "/sales/returns/create", "/sales/returns/confirm"})
public class SalesReturnPageServlet extends BaseServlet {
    private final SalesReturnService service = new SalesReturnService();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/sales/returns".equals(req.getServletPath())) { resp.sendError(405); return; }
        Set<Role> roles = currentUser(req).getRoles();
        boolean create = PermissionPolicy.canTakeOrders(roles);
        boolean confirm = roles.contains(Role.WH_MANAGER) || roles.contains(Role.ADMIN);
        if (!create && !confirm) { resp.sendError(403); return; }
        try {
            req.setAttribute("canCreateReturn", create);
            req.setAttribute("canConfirmReturn", confirm);
            req.setAttribute("returnData", Json.encode(service.page()).replace("<", "\\u003c"));
            render(req, resp, "sales/returns");
        } catch (SQLException e) { throw new ServletException(e); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            SalesReturnService.Saved saved;
            if ("/sales/returns/create".equals(req.getServletPath())) {
                saved = service.create(longParam(req, "invoiceId"), lines(req), currentUser(req));
            } else if ("/sales/returns/confirm".equals(req.getServletPath())) {
                saved = service.confirm(longParam(req, "returnId"), currentUser(req));
            } else { error(resp, 405, "Thao tác không hợp lệ."); return; }
            resp.getWriter().write(Json.encode(saved));
        } catch (ForbiddenException e) { error(resp, 403, e.getMessage());
        } catch (ServiceException | IllegalArgumentException e) { error(resp, 400, e.getMessage());
        } catch (SQLException e) { log("Không xử lý được phiếu trả", e); error(resp, 500, "Không thể xử lý phiếu trả. Vui lòng thử lại."); }
    }

    private static List<SalesReturnService.LineInput> lines(HttpServletRequest req) {
        String[] ids=values(req,"invoiceLineId"), qty=values(req,"quantity"), reasons=values(req,"reason"), conditions=values(req,"condition");
        if(ids.length!=qty.length || ids.length!=reasons.length || ids.length!=conditions.length) throw new ServiceException("Dữ liệu dòng hàng không hợp lệ.");
        List<SalesReturnService.LineInput> result=new ArrayList<>();
        for(int i=0;i<ids.length;i++) try { result.add(new SalesReturnService.LineInput(Long.parseLong(ids[i]),new BigDecimal(qty[i]),reasons[i],conditions[i])); }
        catch(NumberFormatException e){ throw new ServiceException("Dòng "+(i+1)+" có số lượng không hợp lệ."); }
        return result;
    }
    private static String[] values(HttpServletRequest req,String name){String[] v=req.getParameterValues(name);return v==null?new String[0]:v;}
    private static void error(HttpServletResponse resp,int status,String message)throws IOException{resp.setStatus(status);resp.getWriter().write(Json.encode(Map.of("error",message)));}
}
