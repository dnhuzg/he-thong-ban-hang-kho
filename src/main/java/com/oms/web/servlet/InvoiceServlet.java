package com.oms.web.servlet;

import com.oms.model.Invoice;
import com.oms.service.ForbiddenException;
import com.oms.service.InvoiceService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** GHTK-67: lập hoá đơn từ phiếu xuất đã giao, xem và in / tải PDF hoá đơn. */
@WebServlet({"/finance/invoices", "/finance/invoices/view", "/finance/invoices/pdf", "/finance/invoices/issue"})
public class InvoiceServlet extends BaseServlet {

    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/finance/invoices" -> {
                    req.setAttribute("invoices", invoiceService.page(currentUser(req)));
                    render(req, resp, "finance/invoices");
                }
                case "/finance/invoices/view" -> {
                    req.setAttribute("invoice", invoiceService.get(longParam(req, "id"), currentUser(req)));
                    render(req, resp, "finance/invoice-view");
                }
                case "/finance/invoices/pdf" -> {
                    long id = longParam(req, "id");
                    byte[] pdf = invoiceService.pdf(id, currentUser(req));
                    Invoice invoice = invoiceService.get(id, currentUser(req));
                    resp.setContentType("application/pdf");
                    resp.setHeader("Content-Disposition", "inline; filename=\"" + invoice.getCode() + ".pdf\"");
                    resp.setContentLength(pdf.length);
                    resp.getOutputStream().write(pdf);
                }
                default -> resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } catch (InvoiceService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/finance/invoices/issue".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String back = "/finance/invoices";
        try {
            long id = invoiceService.issue(shipmentIds(req), currentUser(req));
            flash(req, "Đã phát hành hoá đơn; công nợ của đại lý đã được ghi nhận.");
            back = "/finance/invoices/view?id=" + id;
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, back);
    }

    private static List<Long> shipmentIds(HttpServletRequest req) {
        List<Long> ids = new ArrayList<>();
        String[] values = req.getParameterValues("shipmentId");
        if (values != null) {
            for (String value : values) {
                try {
                    ids.add(Long.parseLong(value.trim()));
                } catch (NumberFormatException e) {
                    throw new ServiceException("Phiếu xuất được chọn không hợp lệ.");
                }
            }
        }
        return ids;
    }
}
