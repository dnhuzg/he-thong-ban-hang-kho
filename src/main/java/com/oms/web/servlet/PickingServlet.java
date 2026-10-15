
package com.oms.web.servlet;

import com.oms.dao.PickingLotDao;
import com.oms.model.FefoLot;
import com.oms.model.PickingList;
import com.oms.service.ForbiddenException;
import com.oms.service.PickingService;
import com.oms.service.ServiceException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phiếu soạn hàng (S6-03) và chọn lô theo FEFO (S6-04).
 */
@WebServlet({
        "/shipments/picking",
        "/shipments/picking/view",
        "/shipments/picking/create",
        "/shipments/picking/pick",
        "/shipments/picking/complete"
})
public class PickingServlet extends BaseServlet {

    private final PickingService pickingService = new PickingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            switch (req.getServletPath()) {
                case "/shipments/picking" -> {
                    req.setAttribute(
                            "picking",
                            pickingService.page(currentUser(req))
                    );
                    render(req, resp, "shipments/picking");
                }

                case "/shipments/picking/view" -> {
                    long listId = longParam(req, "id");

                    PickingList list = pickingService.get(
                            listId, currentUser(req));

                    Map<Long, List<FefoLot>> lotsByLine = new HashMap<>();

                    for (PickingList.Line line : list.getLines()) {
                        lotsByLine.put(
                                line.getId(),
                                pickingService.suggestLots(
                                        listId,
                                        line.getId(),
                                        currentUser(req)
                                )
                        );
                    }

                    req.setAttribute("list", list);
                    req.setAttribute("fefoLotsByLine", lotsByLine);

                    render(req, resp, "shipments/picking-view");
                }

                default -> resp.sendError(
                        HttpServletResponse.SC_METHOD_NOT_ALLOWED
                );
            }
        } catch (PickingService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        long listId = longParam(req, "id");
        String back = "/shipments/picking/view?id=" + listId;

        try {
            switch (req.getServletPath()) {
                case "/shipments/picking/create" -> {
                    back = "/shipments/picking";

                    long id = pickingService.create(
                            orderIds(req),
                            currentUser(req)
                    );

                    flash(
                            req,
                            "Đã lập phiếu soạn; các đơn chuyển sang Đang soạn hàng."
                    );

                    back = "/shipments/picking/view?id=" + id;
                }

                case "/shipments/picking/pick" -> {
                    long lineId = longParam(req, "lineId");
                    back += "#line-" + lineId;

                    BigDecimal picked = quantity(param(req, "picked"));
                    String shortReason = param(req, "shortReason");

                    String[] lotIds =
                            req.getParameterValues("lotId");

                    String[] lotQuantities =
                            req.getParameterValues("lotQuantity");

                    String[] overrideReasons =
                            req.getParameterValues("overrideReason");

                    /*
                     * S6-04:
                     * Luôn kiểm tra và lưu danh sách lô.
                     *
                     * Không quay về pick() cũ khi không chọn lô,
                     * vì điều đó cho phép ghi số lượng đã soạn
                     * nhưng không có phân bổ lô tương ứng.
                     */
                    List<PickingLotDao.Selection> selectedLots =
                            lotIds == null
                                    ? List.of()
                                    : selections(
                                            lotIds,
                                            lotQuantities,
                                            overrideReasons
                                    );

                    pickingService.pickWithLots(
                            listId,
                            lineId,
                            picked,
                            shortReason,
                            selectedLots,
                            currentUser(req)
                    );
                }

                case "/shipments/picking/complete" -> {
                    pickingService.complete(
                            listId,
                            currentUser(req)
                    );

                    flash(
                            req,
                            "Đã hoàn tất phiếu soạn; các đơn sẵn sàng xuất kho."
                    );
                }

                default -> {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        } catch (PickingService.NotFoundException e) {
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

        redirect(req, resp, back);
    }

    private static List<PickingLotDao.Selection> selections(
            String[] lotIds,
            String[] quantities,
            String[] reasons) {

        if (quantities == null
                || lotIds.length != quantities.length
                || (reasons != null && lotIds.length != reasons.length)) {
            throw new ServiceException(
                    "Thông tin lô hàng không hợp lệ."
            );
        }

        List<PickingLotDao.Selection> result = new ArrayList<>();

        for (int i = 0; i < lotIds.length; i++) {
            String idValue = lotIds[i] == null
                    ? ""
                    : lotIds[i].trim();

            String quantityValue = quantities[i] == null
                    ? ""
                    : quantities[i].trim();

            if (idValue.isEmpty() || quantityValue.isEmpty()) {
                throw new ServiceException(
                        "Phải chọn lô và nhập số lượng lấy từ lô."
                );
            }

            long lotId;
            try {
                lotId = Long.parseLong(idValue);
            } catch (NumberFormatException e) {
                throw new ServiceException(
                        "Mã lô được chọn không hợp lệ."
                );
            }

            String overrideReason =
                    reasons == null ? null : reasons[i];

            result.add(
                    new PickingLotDao.Selection(
                            lotId,
                            quantity(quantityValue),
                            overrideReason
                    )
            );
        }

        return result;
    }

    private static List<Long> orderIds(HttpServletRequest req) {
        List<Long> ids = new ArrayList<>();
        String[] values = req.getParameterValues("orderId");

        if (values != null) {
            for (String value : values) {
                try {
                    ids.add(Long.parseLong(value.trim()));
                } catch (NumberFormatException e) {
                    throw new ServiceException(
                            "Đơn được chọn không hợp lệ."
                    );
                }
            }
        }

        return ids;
    }

    private static BigDecimal quantity(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(
                    value.trim().replace(',', '.')
            );
        } catch (NumberFormatException e) {
            throw new ServiceException(
                    "Số lượng không hợp lệ."
            );
        }
    }
}
