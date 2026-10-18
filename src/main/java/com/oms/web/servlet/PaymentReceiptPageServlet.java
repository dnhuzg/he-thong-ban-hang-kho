package com.oms.web.servlet;

import com.oms.model.Payment;
import com.oms.service.ForbiddenException;
import com.oms.service.PaymentReceiptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** GHTK-75: danh sách phiếu thu đã ghi nhận, xem trước và in / tải PDF từng phiếu. */
@WebServlet({"/finance/payment-receipts", "/finance/payment-receipts/pdf"})
public class PaymentReceiptPageServlet extends BaseServlet {

    private final PaymentReceiptService receiptService = new PaymentReceiptService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long id = longParam(req, "id");
        try {
            if ("/finance/payment-receipts/pdf".equals(req.getServletPath())) {
                Payment payment = receiptService.get(id, currentUser(req));
                byte[] pdf = receiptService.pdf(id, currentUser(req));
                resp.setContentType("application/pdf");
                resp.setHeader("Content-Disposition", ("1".equals(param(req, "download")) ? "attachment" : "inline")
                        + "; filename=\"phieu-thu-" + payment.getCode() + ".pdf\"");
                resp.setContentLength(pdf.length);
                resp.getOutputStream().write(pdf);
                return;
            }
            List<Payment> receipts = receiptService.list(currentUser(req));
            req.setAttribute("receipts", receipts);
            if (id > 0) {
                req.setAttribute("selected", receiptService.get(id, currentUser(req)));
            } else if (!receipts.isEmpty()) {
                req.setAttribute("selected", receipts.get(0));
            }
            resp.setHeader("Cache-Control", "no-store");
            render(req, resp, "finance/payment-receipts");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (PaymentReceiptService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
