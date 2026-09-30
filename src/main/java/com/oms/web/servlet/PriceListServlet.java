package com.oms.web.servlet;

import com.oms.model.Module;
import com.oms.model.PriceList;
import com.oms.service.PermissionService;
import com.oms.service.PriceListService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Bảng giá theo nhóm khách hàng (S2-10): danh sách, tạo, xem / sửa dòng giá, tạo phiên bản mới. */
@WebServlet({"/products/price-lists", "/products/price-lists/new", "/products/price-lists/view",
        "/products/price-lists/header", "/products/price-lists/items/save", "/products/price-lists/items/delete",
        "/products/price-lists/new-version"})
public class PriceListServlet extends BaseServlet {

    private final PriceListService priceListService = new PriceListService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            if (ownScopeOnly(req)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            req.setAttribute("groups", priceListService.customerGroups());
            req.setAttribute("canWrite", permissionService.policy().canWrite(currentUser(req).getRoles(), Module.PRODUCT));
            switch (req.getServletPath()) {
                case "/products/price-lists" -> {
                    long groupId = longParam(req, "group");
                    req.setAttribute("canViewPriceLists", true);
                    req.setAttribute("selectedGroup", groupId);
                    req.setAttribute("lists", priceListService.list(groupId > 0 ? groupId : null));
                    render(req, resp, "price-lists/list");
                }
                case "/products/price-lists/new" -> {
                    req.setAttribute("form", new PriceList());
                    render(req, resp, "price-lists/form");
                }
                case "/products/price-lists/view" -> showView(req, resp, longParam(req, "id"));
                default -> redirect(req, resp, "/products/price-lists");
            }
        } catch (PriceListService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long id = longParam(req, "id");
        try {
            switch (req.getServletPath()) {
                case "/products/price-lists/new" -> {
                    long newId = priceListService.create(readHeader(req), currentUser(req));
                    flash(req, "Đã tạo bảng giá. Thêm các dòng giá bên dưới.");
                    redirect(req, resp, "/products/price-lists/view?id=" + newId);
                    return;
                }
                case "/products/price-lists/header" -> {
                    priceListService.updateHeader(id, readHeader(req), currentUser(req));
                    flash(req, "Đã cập nhật thông tin bảng giá.");
                }
                case "/products/price-lists/items/save" -> {
                    priceListService.saveItem(id, longParam(req, "productId"), param(req, "salePrice"), param(req, "floorPrice"), currentUser(req));
                    flash(req, "Đã lưu dòng giá.");
                }
                case "/products/price-lists/items/delete" -> {
                    priceListService.deleteItem(id, longParam(req, "productId"), currentUser(req));
                    flash(req, "Đã xoá dòng giá.");
                }
                case "/products/price-lists/new-version" -> {
                    long newId = priceListService.newVersion(id, date(param(req, "validFrom")), currentUser(req));
                    flash(req, "Đã tạo phiên bản mới, chép đủ các dòng giá từ phiên bản trước.");
                    redirect(req, resp, "/products/price-lists/view?id=" + newId);
                    return;
                }
                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
            redirect(req, resp, "/products/price-lists/view?id=" + id);
        } catch (PriceListService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ServiceException e) {
            if (req.getServletPath().equals("/products/price-lists/new")) {
                req.setAttribute("error", e.getMessage());
                try {
                    req.setAttribute("groups", priceListService.customerGroups());
                } catch (SQLException ex) {
                    throw new ServletException(ex);
                }
                req.setAttribute("form", readHeaderLenient(req));
                render(req, resp, "price-lists/form");
                return;
            }
            flash(req, e.getMessage());
            redirect(req, resp, "/products/price-lists/view?id=" + id);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showView(HttpServletRequest req, HttpServletResponse resp, long id) throws SQLException, ServletException, IOException {
        PriceList pl = priceListService.get(id, currentUser(req));
        req.setAttribute("priceList", pl);
        req.setAttribute("versions", priceListService.versions(pl.getCode()));
        req.setAttribute("products", priceListService.productOptions());
        render(req, resp, "price-lists/view");
    }

    /** Đại lý (quyền "chỉ dữ liệu của mình") chưa có cổng riêng: không cho xem bảng giá của mọi nhóm. */
    private boolean ownScopeOnly(HttpServletRequest req) throws SQLException {
        return permissionService.policy().isOwnScopeOnly(currentUser(req).getRoles(), Module.PRODUCT);
    }

    private static PriceList readHeader(HttpServletRequest req) {
        PriceList pl = new PriceList();
        pl.setCode(param(req, "code"));
        pl.setName(param(req, "name"));
        pl.setCustomerGroupId(longParam(req, "customerGroupId"));
        pl.setValidFrom(date(param(req, "validFrom")));
        pl.setValidTo(date(param(req, "validTo")));
        return pl;
    }

    /** Dùng khi hiển thị lại form sau lỗi: ngày sai định dạng thì để trống thay vì ném lỗi lần nữa. */
    private static PriceList readHeaderLenient(HttpServletRequest req) {
        PriceList pl = new PriceList();
        pl.setCode(param(req, "code"));
        pl.setName(param(req, "name"));
        pl.setCustomerGroupId(longParam(req, "customerGroupId"));
        try {
            pl.setValidFrom(date(param(req, "validFrom")));
            pl.setValidTo(date(param(req, "validTo")));
        } catch (ServiceException ignored) {
            // giữ trống ngày sai định dạng
        }
        return pl;
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
