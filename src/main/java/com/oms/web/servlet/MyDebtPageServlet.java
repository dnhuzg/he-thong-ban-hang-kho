package com.oms.web.servlet;

import com.oms.model.Invoice;
import com.oms.service.ForbiddenException;
import com.oms.service.InvoiceService;
import com.oms.service.MyDebtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** GHTK-73: đại lý xem công nợ, hạn mức còn lại và tải lại hoá đơn của chính mình. */
@WebServlet({"/portal/my-debt", "/portal/my-debt/invoice"})
public class MyDebtPageServlet extends BaseServlet {

    private final MyDebtService debtService = new MyDebtService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        try {
            if ("/portal/my-debt/invoice".equals(req.getServletPath())) {
                long id = longParam(req, "id");
                Invoice invoice = invoiceService.getForCustomer(id, currentUser(req));
                byte[] pdf = invoiceService.pdfForCustomer(id, currentUser(req));
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", "inline; filename=\"" + invoice.getCode() + ".pdf\"");
                resp.setContentLength(pdf.length);
                resp.getOutputStream().write(pdf);
                return;
            }
            req.setAttribute("debt", debtService.view(currentUser(req)));
            render(req, resp, "portal/my-debt");
        } catch (InvoiceService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
