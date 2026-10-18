
package com.oms.web.servlet;

import com.oms.service.DeliveryTripPdfService;
import com.oms.service.DeliveryTripService;
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

/**
 * GHTK-63 / S6-06: Phân tuyến giao hàng và gán tài xế.
 */
@WebServlet({
        "/shipments/trips",
        "/shipments/trips/create",
        "/shipments/trips/view",
        "/shipments/trips/assign",
        "/shipments/trips/print"
})
public class DeliveryTripServlet extends BaseServlet {

    private final DeliveryTripService service =
            new DeliveryTripService();

    private final DeliveryTripPdfService pdfService =
            new DeliveryTripPdfService();

    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        try {
            switch (req.getServletPath()) {

                case "/shipments/trips" -> {
                    req.setAttribute(
                            "page",
                            service.page(currentUser(req))
                    );
                    render(req, resp, "shipments/trips");
                }

                case "/shipments/trips/view" -> {
                    long tripId = longParam(req, "id");

                    req.setAttribute(
                            "detail",
                            service.get(tripId, currentUser(req))
                    );

                    DeliveryTripService.Page page =
                            service.page(currentUser(req));

                    req.setAttribute("drivers", page.drivers());
                    req.setAttribute("vehicles", page.vehicles());

                    render(req, resp, "shipments/trip-view");
                }

                /*
                 * AC3: Xuất file PDF danh sách điểm giao.
                 *
                 * GET /shipments/trips/print?id=1
                 */
                case "/shipments/trips/print" -> {
                    long tripId = longParam(req, "id");

                    byte[] pdf = pdfService.export(
                            tripId,
                            currentUser(req)
                    );

                    resp.setContentType("application/pdf");
                    resp.setHeader(
                            "Content-Disposition",
                            "inline; filename=\"delivery-trip-"
                                    + tripId + ".pdf\""
                    );
                    resp.setContentLength(pdf.length);
                    resp.setHeader("Cache-Control", "no-store");

                    resp.getOutputStream().write(pdf);
                }

                default -> resp.sendError(
                        HttpServletResponse.SC_METHOD_NOT_ALLOWED
                );
            }

        } catch (DeliveryTripService.NotFoundException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (ForbiddenException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);

        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws ServletException, IOException {

        String back = "/shipments/trips";

        try {
            switch (req.getServletPath()) {

                /*
                 * AC1: Tạo chuyến giao từ nhiều phiếu xuất.
                 */
                case "/shipments/trips/create" -> {
                    List<Long> shipmentIds = shipmentIds(req);

                    LocalDate deliveryDate = deliveryDate(req);

                    Long driverId =
                            optionalId(req, "driverId");

                    Long vehicleId =
                            optionalId(req, "vehicleId");

                    long tripId = service.create(
                            shipmentIds,
                            deliveryDate,
                            driverId,
                            vehicleId,
                            currentUser(req)
                    );

                    flash(
                            req,
                            "Đã lập chuyến giao hàng thành công."
                    );

                    back = "/shipments/trips/view?id=" + tripId;
                }

                /*
                 * AC2: Gán tài xế và phương tiện.
                 */
                case "/shipments/trips/assign" -> {
                    long tripId = longParam(req, "id");

                    back = "/shipments/trips/view?id=" + tripId;

                    Long driverId =
                            optionalId(req, "driverId");

                    Long vehicleId =
                            optionalId(req, "vehicleId");

                    if (driverId == null || vehicleId == null) {
                        throw new ServiceException(
                                "Vui lòng chọn cả tài xế và phương tiện."
                        );
                    }

                    service.assign(
                            tripId,
                            driverId,
                            vehicleId,
                            currentUser(req)
                    );

                    flash(
                            req,
                            "Đã cập nhật tài xế và phương tiện."
                    );
                }

                default -> {
                    resp.sendError(
                            HttpServletResponse.SC_NOT_FOUND
                    );
                    return;
                }
            }

        } catch (DeliveryTripService.NotFoundException e) {
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

    private static List<Long> shipmentIds(
            HttpServletRequest req
    ) {
        String[] values =
                req.getParameterValues("shipmentId");

        List<Long> ids = new ArrayList<>();

        if (values == null) {
            return ids;
        }

        for (String value : values) {
            try {
                long id = Long.parseLong(value.trim());

                if (id <= 0) {
                    throw new NumberFormatException();
                }

                ids.add(id);

            } catch (NumberFormatException
                     | NullPointerException e) {

                throw new ServiceException(
                        "Phiếu giao hàng được chọn không hợp lệ."
                );
            }
        }

        return ids;
    }

    private static LocalDate deliveryDate(
            HttpServletRequest req
    ) {
        String value = param(req, "deliveryDate");

        try {
            return LocalDate.parse(value);

        } catch (DateTimeParseException
                 | NullPointerException e) {

            throw new ServiceException(
                    "Ngày giao hàng không hợp lệ."
            );
        }
    }

    private static Long optionalId(
            HttpServletRequest req,
            String name
    ) {
        String value = param(req, name);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            long id = Long.parseLong(value.trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException e) {
            throw new ServiceException(
                    "Giá trị " + name + " không hợp lệ."
            );
        }
    }
}
