package com.oms.web.servlet;
import com.oms.service.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/orders/pdf")
public class OrderPdfServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        long id=longParam(req,"id");
        try {
            byte[] pdf=new OrderPdfService().export(id,currentUser(req));
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition","inline; filename=\"order-"+id+".pdf\"");
            resp.setHeader("Cache-Control","no-store"); resp.setContentLength(pdf.length); resp.getOutputStream().write(pdf);
        } catch(OrderService.NotFoundException e) { resp.sendError(404); }
        catch(ForbiddenException e) { resp.sendError(403); }
        catch(ServiceException e) { resp.sendError(400,"Đơn không đủ dữ liệu để xuất PDF."); }
        catch(SQLException | IOException e) { log("Không xuất được PDF đơn hàng",e); resp.sendError(500,"Không thể xuất đơn. Vui lòng thử lại."); }
    }
}
