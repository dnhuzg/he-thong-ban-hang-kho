package com.oms.web.servlet;

import com.oms.model.Payment;
import com.oms.service.ForbiddenException;
import com.oms.service.PaymentService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** GHTK-68: ghi nhận thanh toán của đại lý và đối trừ vào hoá đơn. */
@WebServlet({"/finance/payments", "/finance/payments/record"})
public class PaymentServlet extends BaseServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/finance/payments".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        try {
            req.setAttribute("payments", paymentService.page(currentUser(req)));
            req.setAttribute("today", LocalDate.now().toString());
            render(req, resp, "finance/payments");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/finance/payments/record".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            paymentService.record(form(req), currentUser(req));
            flash(req, "Đã ghi nhận thanh toán và đối trừ vào công nợ.");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/finance/payments");
    }

    private static PaymentService.Form form(HttpServletRequest req) {
        Payment.Method method;
        try {
            method = param(req, "method").isEmpty() ? null : Payment.Method.valueOf(param(req, "method"));
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Hình thức thu không hợp lệ.");
        }
        BigDecimal amount;
        try {
            amount = param(req, "amount").isEmpty() ? null : new BigDecimal(param(req, "amount"));
        } catch (NumberFormatException e) {
            throw new ServiceException("Số tiền không hợp lệ.");
        }
        LocalDate paidOn;
        try {
            paidOn = param(req, "paidOn").isEmpty() ? null : LocalDate.parse(param(req, "paidOn"));
        } catch (DateTimeParseException e) {
            throw new ServiceException("Ngày thu không hợp lệ.");
        }
        return new PaymentService.Form(id(req, "customerId"), id(req, "invoiceId"), amount, method, paidOn,
                id(req, "collectedBy"), param(req, "note"));
    }

    private static Long id(HttpServletRequest req, String name) {
        long value = longParam(req, name);
        return value > 0 ? value : null;
    }
}
