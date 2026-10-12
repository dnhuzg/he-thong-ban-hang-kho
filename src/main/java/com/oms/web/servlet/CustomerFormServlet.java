package com.oms.web.servlet;

import com.oms.model.Customer;
import com.oms.security.PermissionPolicy;
import com.oms.service.CustomerService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Tạo / sửa / xoá đại lý (S3-03): chỉ Admin, Quản lý kinh doanh và Kế toán công nợ. */
@WebServlet({"/customers/new", "/customers/edit", "/customers/delete"})
public class CustomerFormServlet extends BaseServlet {

    private final CustomerService customerService = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!canManage(req)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (req.getServletPath().equals("/customers/delete")) {
            redirect(req, resp, "/customers");
            return;
        }
        try {
            Customer customer = isEdit(req) ? customerService.get(longParam(req, "id"), currentUser(req)) : new Customer();
            show(req, resp, customer);
        } catch (CustomerService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = longParam(req, "id");
        try {
            if (req.getServletPath().equals("/customers/delete")) {
                customerService.delete(id, currentUser(req));
                flash(req, "Đã xoá đại lý.");
                redirect(req, resp, "/customers");
                return;
            }
            Customer input = readForm(req);
            if (isEdit(req)) {
                customerService.update(id, input, currentUser(req));
                flash(req, "Đã cập nhật hồ sơ đại lý.");
                redirect(req, resp, "/customers/view?id=" + id);
            } else {
                long newId = customerService.create(input, currentUser(req));
                flash(req, "Đã thêm đại lý " + customerService.get(newId, currentUser(req)).getCode() + ".");
                redirect(req, resp, "/customers/view?id=" + newId);
            }
        } catch (CustomerService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (ServiceException e) {
            if (req.getServletPath().equals("/customers/delete")) {
                flash(req, e.getMessage());
                redirect(req, resp, "/customers");
                return;
            }
            req.setAttribute("error", e.getMessage());
            Customer input = readForm(req);
            input.setId(id);
            show(req, resp, input);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, Customer customer) throws ServletException, IOException {
        try {
            req.setAttribute("customer", customer);
            req.setAttribute("isNew", !isEdit(req));
            req.setAttribute("groups", customerService.groups());
            req.setAttribute("areas", customerService.areas());
            req.setAttribute("reps", customerService.salesReps());
            req.setAttribute("canAssign", PermissionPolicy.canAssignCustomers(currentUser(req).getRoles()));
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        render(req, resp, "customers/form");
    }

    private boolean canManage(HttpServletRequest req) {
        return PermissionPolicy.canManageCustomers(currentUser(req).getRoles());
    }

    private static boolean isEdit(HttpServletRequest req) {
        return req.getServletPath().equals("/customers/edit");
    }

    private static Customer readForm(HttpServletRequest req) {
        Customer c = new Customer();
        c.setCode(param(req, "code"));
        c.setName(param(req, "name"));
        c.setTaxCode(param(req, "taxCode"));
        c.setPhone(param(req, "phone"));
        c.setGroupId(idOrNull(req, "groupId"));
        c.setAreaId(idOrNull(req, "areaId"));
        c.setSalesRepId(idOrNull(req, "salesRepId"));
        return c;
    }

    private static Long idOrNull(HttpServletRequest req, String name) {
        long v = longParam(req, name);
        return v > 0 ? v : null;
    }
}
