package com.oms.web.servlet;

import com.oms.model.Supplier;
import com.oms.service.ServiceException;
import com.oms.service.SupplierService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/** Tạo / sửa / xoá nhà cung cấp (S2-09). */
@WebServlet({"/suppliers/new", "/suppliers/edit", "/suppliers/delete"})
public class SupplierFormServlet extends BaseServlet {

    private final SupplierService supplierService = new SupplierService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getServletPath().equals("/suppliers/delete")) {
            redirect(req, resp, "/suppliers");
            return;
        }
        try {
            Supplier supplier = isEdit(req) ? supplierService.get(longParam(req, "id")) : new Supplier();
            show(req, resp, supplier);
        } catch (SupplierService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = longParam(req, "id");
        try {
            if (req.getServletPath().equals("/suppliers/delete")) {
                supplierService.delete(id, currentUser(req));
                flash(req, "Đã xoá nhà cung cấp.");
                redirect(req, resp, "/suppliers");
                return;
            }
            Supplier input = readForm(req);
            if (isEdit(req)) {
                supplierService.update(id, input, currentUser(req));
                flash(req, "Đã cập nhật nhà cung cấp " + supplierService.get(id).getCode() + ".");
            } else {
                long newId = supplierService.create(input, currentUser(req));
                flash(req, "Đã thêm nhà cung cấp " + supplierService.get(newId).getCode() + ".");
            }
            redirect(req, resp, "/suppliers");
        } catch (SupplierService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            if (req.getServletPath().equals("/suppliers/delete")) {
                flash(req, e.getMessage());
                redirect(req, resp, "/suppliers");
                return;
            }
            req.setAttribute("error", e.getMessage());
            Supplier input = readForm(req);
            input.setId(id);
            show(req, resp, input);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void show(HttpServletRequest req, HttpServletResponse resp, Supplier supplier) throws ServletException, IOException {
        req.setAttribute("supplier", supplier);
        req.setAttribute("isNew", !isEdit(req));
        req.setAttribute("statuses", Supplier.Status.values());
        render(req, resp, "suppliers/form");
    }

    private static boolean isEdit(HttpServletRequest req) {
        return req.getServletPath().equals("/suppliers/edit");
    }

    private static Supplier readForm(HttpServletRequest req) {
        Supplier s = new Supplier();
        s.setCode(param(req, "code"));
        s.setName(param(req, "name"));
        s.setTaxCode(param(req, "taxCode"));
        s.setContactName(param(req, "contactName"));
        s.setContactPhone(param(req, "contactPhone"));
        s.setEmail(param(req, "email"));
        s.setAddress(param(req, "address"));
        s.setPaymentTerms(param(req, "paymentTerms"));
        try {
            s.setStatus(Supplier.Status.valueOf(param(req, "status")));
        } catch (IllegalArgumentException e) {
            s.setStatus(Supplier.Status.ACTIVE);
        }
        return s;
    }
}
