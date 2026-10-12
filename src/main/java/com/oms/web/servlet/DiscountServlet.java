package com.oms.web.servlet;

import com.oms.model.DiscountPolicy;
import com.oms.model.DiscountTier;
import com.oms.service.DiscountService;
import com.oms.service.ForbiddenException;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Chính sách chiết khấu theo sản lượng (S3-01): danh sách, tạo, sửa, xoá; chỉ Quản lý kinh doanh và Admin. */
@WebServlet({"/products/discounts", "/products/discounts/new", "/products/discounts/edit", "/products/discounts/delete"})
public class DiscountServlet extends BaseServlet {

    /** Số hàng bậc tối thiểu trong form; chính sách đã có nhiều bậc thì thêm một hàng trống sau bậc cuối. */
    private static final int MIN_TIER_ROWS = 5;

    private final DiscountService discountService = new DiscountService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/products/discounts" -> {
                    req.setAttribute("policies", discountService.list(currentUser(req)));
                    req.setAttribute("canViewPriceLists", true);
                    render(req, resp, "products/discounts");
                }
                case "/products/discounts/new" -> {
                    discountService.list(currentUser(req)); // chặn người không có quyền trước khi dựng form
                    showForm(req, resp, new DiscountPolicy(), tierRows(List.of()));
                }
                case "/products/discounts/edit" -> {
                    DiscountPolicy p = discountService.get(longParam(req, "id"), currentUser(req));
                    showForm(req, resp, p, tierRows(p.getTiers()));
                }
                default -> redirect(req, resp, "/products/discounts");
            }
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (DiscountService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/products/discounts/new" -> {
                    discountService.save(0, readPolicy(req), readTierRows(req), currentUser(req));
                    flash(req, "Đã tạo chính sách chiết khấu.");
                }
                case "/products/discounts/edit" -> {
                    discountService.save(longParam(req, "id"), readPolicy(req), readTierRows(req), currentUser(req));
                    flash(req, "Đã lưu chính sách chiết khấu.");
                }
                case "/products/discounts/delete" -> {
                    discountService.delete(longParam(req, "id"), currentUser(req));
                    flash(req, "Đã xoá chính sách chiết khấu.");
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            redirect(req, resp, "/products/discounts");
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (DiscountService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            if (path.equals("/products/discounts/delete")) {
                flash(req, e.getMessage());
                redirect(req, resp, "/products/discounts");
                return;
            }
            req.setAttribute("error", e.getMessage());
            DiscountPolicy form = readPolicyLenient(req);
            form.setId(longParam(req, "id"));
            try {
                showForm(req, resp, form, padRows(readTierRows(req)));
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, DiscountPolicy form, List<String[]> rows)
            throws SQLException, ServletException, IOException {
        req.setAttribute("form", form);
        req.setAttribute("tierRows", rows);
        req.setAttribute("products", discountService.productOptions());
        req.setAttribute("categories", discountService.categories());
        req.setAttribute("types", DiscountPolicy.Type.values());
        req.setAttribute("statuses", DiscountPolicy.Status.values());
        render(req, resp, "products/discount-form");
    }

    private static DiscountPolicy readPolicy(HttpServletRequest req) {
        DiscountPolicy p = readPolicyLenient(req);
        p.setValidFrom(date(param(req, "validFrom")));
        p.setValidTo(date(param(req, "validTo")));
        return p;
    }

    /** Dùng khi hiển thị lại form sau lỗi: ngày sai định dạng thì để trống thay vì ném lỗi lần nữa. */
    private static DiscountPolicy readPolicyLenient(HttpServletRequest req) {
        DiscountPolicy p = new DiscountPolicy();
        p.setName(param(req, "name"));
        boolean product = "product".equals(param(req, "scope"));
        long productId = longParam(req, "productId");
        long categoryId = longParam(req, "categoryId");
        p.setProductId(product && productId > 0 ? productId : null);
        p.setCategoryId(!product && categoryId > 0 ? categoryId : null);
        // phạm vi đã chọn nhưng chưa chọn sản phẩm / nhóm: giữ lựa chọn để form không nhảy về nhóm hàng
        req.setAttribute("scopeIsProduct", product);
        for (DiscountPolicy.Type t : DiscountPolicy.Type.values()) {
            if (t.name().equals(param(req, "type"))) {
                p.setType(t);
            }
        }
        for (DiscountPolicy.Status s : DiscountPolicy.Status.values()) {
            if (s.name().equals(param(req, "status"))) {
                p.setStatus(s);
            }
        }
        try {
            p.setValidFrom(date(param(req, "validFrom")));
            p.setValidTo(date(param(req, "validTo")));
        } catch (ServiceException ignored) {
            // giữ trống ngày sai định dạng
        }
        return p;
    }

    private static List<String[]> readTierRows(HttpServletRequest req) {
        String[] qty = req.getParameterValues("tierQty");
        String[] value = req.getParameterValues("tierValue");
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; qty != null && i < qty.length; i++) {
            rows.add(new String[]{qty[i], value != null && i < value.length ? value[i] : ""});
        }
        return rows;
    }

    private static List<String[]> tierRows(List<DiscountTier> tiers) {
        List<String[]> rows = new ArrayList<>();
        for (DiscountTier t : tiers) {
            rows.add(new String[]{String.valueOf(t.getMinQty()), t.getValue().stripTrailingZeros().toPlainString()});
        }
        if (!rows.isEmpty() && rows.size() < DiscountService.MAX_TIERS) {
            rows.add(new String[]{"", ""});
        }
        return padRows(rows);
    }

    private static List<String[]> padRows(List<String[]> rows) {
        int target = Math.max(MIN_TIER_ROWS, rows.size());
        List<String[]> padded = new ArrayList<>(rows);
        while (padded.size() < target) {
            padded.add(new String[]{"", ""});
        }
        return padded;
    }

    private static LocalDate date(String text) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ServiceException("Ngày không hợp lệ: " + text);
        }
    }
}
