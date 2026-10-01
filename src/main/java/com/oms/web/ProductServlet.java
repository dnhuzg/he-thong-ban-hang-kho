package com.oms.web;

import com.oms.model.Product;
import com.oms.model.User;
import com.oms.service.ProductService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    private ProductService productService = new ProductService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        
        String action = req.getParameter("action");
        if (action == null) {
            action = "list";
        }

        try {
            switch (action) {
                case "delete":
                    handleDelete(req, resp);
                    break;
                case "list":
                default:
                    handleList(req, resp);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            req.setAttribute("errorMessage", "Đã xảy ra lỗi hệ thống: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/error.jsp").forward(req, resp);
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Chuyển hướng render giao diện JSP danh sách sản phẩm
        req.getRequestDispatcher("/WEB-INF/views/product-list.jsp").forward(req, resp);
    }

    private void handleDelete(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HttpSession session = req.getSession();
        User currentUser = (User) session.getAttribute("currentUser");

        String idStr = req.getParameter("id");
        if (idStr != null && currentUser != null) {
            long productId = Long.parseLong(idStr);
            productService.delete(productId, currentUser);
            session.setAttribute("successMessage", "Xử lý xóa / ngừng kinh doanh sản phẩm thành công!");
        }
        resp.sendRedirect(req.getContextPath() + "/products");
    }
}