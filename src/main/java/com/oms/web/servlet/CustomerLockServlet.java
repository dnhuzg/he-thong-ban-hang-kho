package com.oms.web.servlet;

import com.oms.service.CustomerLockService;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Khoá / mở giao dịch với đại lý (S3-07); lý do là bắt buộc khi khoá. */
@WebServlet({"/customers/lock", "/customers/unlock"})
public class CustomerLockServlet extends BaseServlet {

    private final CustomerLockService lockService = new CustomerLockService();
    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        redirect(req, resp, "/customers");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long customerId = longParam(req, "customerId");
        try {
            // get() chặn người không thấy được đại lý này (Nhân viên kinh doanh của người khác) trước khi đổi trạng thái
            customerService.get(customerId, currentUser(req));
            if (req.getServletPath().equals("/customers/lock")) {
                lockService.lock(customerId, param(req, "reason"), currentUser(req));
                flash(req, "Đã khoá giao dịch với đại lý.");
            } else {
                lockService.unlock(customerId, param(req, "reason"), currentUser(req));
                flash(req, "Đã mở lại giao dịch với đại lý.");
            }
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
        redirect(req, resp, "/customers/view?id=" + customerId);
    }
}
