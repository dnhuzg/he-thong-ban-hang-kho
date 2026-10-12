package com.oms.web.servlet;

import com.oms.service.CreditService;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Đổi hạn mức công nợ và số ngày nợ của đại lý (S3-05); lý do là bắt buộc. */
@WebServlet("/customers/credit/save")
public class CreditServlet extends BaseServlet {

    private final CreditService creditService = new CreditService();
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        redirect(req, resp, "/customers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long customerId = longParam(req, "customerId");
        try {
            // get() chặn người không thấy được đại lý này (Nhân viên kinh doanh của người khác) trước khi sửa
            customerService.get(customerId, currentUser(req));
            creditService.update(customerId, param(req, "creditLimit"), param(req, "creditDays"), param(req, "reason"), currentUser(req));
            flash(req, "Đã cập nhật hạn mức công nợ.");
        } catch (CustomerService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/customers/view?id=" + customerId + "#han-muc-cong-no");
    }
}
