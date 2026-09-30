package com.oms.web.servlet;

import com.oms.model.User;
import com.oms.service.ServiceException;
import com.oms.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Khoá / mở khoá tài khoản (S1-10). */
@WebServlet({"/admin/users/lock", "/admin/users/unlock"})
public class UserLockServlet extends BaseServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = longParam(req, "id");
        User actor = currentUser(req);
        try {
            User target = userService.get(id);
            if (req.getServletPath().equals("/admin/users/lock")) {
                userService.lock(id, param(req, "reason"), actor);
                String message = "Đã khoá tài khoản " + target.getUsername() + " và thu hồi các phiên đang mở.";
                List<String> customers = userService.customerNamesOf(id);
                if (!customers.isEmpty()) {
                    message += " Có " + customers.size() + " đại lý cần bàn giao cho người khác: "
                            + String.join(", ", customers.subList(0, Math.min(5, customers.size())))
                            + (customers.size() > 5 ? "…" : "") + ".";
                }
                flash(req, message);
            } else {
                userService.unlock(id, actor);
                flash(req, "Đã mở khoá tài khoản " + target.getUsername() + ".");
            }
        } catch (UserService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (ServiceException e) {
            flash(req, e.getMessage());
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        redirect(req, resp, "/admin/users");
    }
}
