package com.oms.web.servlet;

import com.oms.model.Customer;
import com.oms.security.PermissionPolicy;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Danh sách đại lý (S3-03) kèm tìm kiếm và lọc (S3-08); Nhân viên kinh doanh chỉ thấy đại lý mình phụ trách. */
@WebServlet("/customers")
public class CustomerListServlet extends BaseServlet {

    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Long groupId = idParam(req, "group");
            Long areaId = idParam(req, "area");
            Long repId = idParam(req, "rep");
            Customer.Status status = statusParam(req);
            String q = param(req, "q");
            req.setAttribute("result", customerService.search(q, groupId, areaId, repId, status,
                    intParam(req, "page", 1), currentUser(req)));
            req.setAttribute("q", q);
            req.setAttribute("selectedGroup", groupId);
            req.setAttribute("selectedArea", areaId);
            req.setAttribute("selectedRep", repId);
            req.setAttribute("selectedStatus", status == null ? "" : status.name());
            req.setAttribute("statuses", Customer.Status.values());
            req.setAttribute("groups", customerService.groups());
            req.setAttribute("areas", customerService.areas());
            req.setAttribute("reps", customerService.salesReps());
            req.setAttribute("canAssign", PermissionPolicy.canAssignCustomers(currentUser(req).getRoles()));
            req.setAttribute("canManage", PermissionPolicy.canManageCustomers(currentUser(req).getRoles()));
            render(req, resp, "customers/list");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /** Mã lọc không có hoặc sai định dạng thì coi như không lọc. */
    private static Long idParam(HttpServletRequest req, String name) {
        long id = longParam(req, name);
        return id > 0 ? id : null;
    }

    private static Customer.Status statusParam(HttpServletRequest req) {
        String s = req.getParameter("status");
        for (Customer.Status v : Customer.Status.values()) {
            if (v.name().equals(s)) {
                return v;
            }
        }
        return null;
    }
}
