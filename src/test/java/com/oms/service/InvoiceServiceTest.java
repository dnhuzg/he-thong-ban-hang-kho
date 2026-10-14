package com.oms.service;

import com.oms.model.Invoice;
import com.oms.model.PickingList;
import com.oms.model.Role;
import com.oms.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceServiceTest extends PickingServiceTest {

    private final ShipmentExportService exports = new ShipmentExportService();
    protected final InvoiceService invoices = new InvoiceService();
    protected User accountant;

    @BeforeEach
    void accountant() throws Exception {
        accountant = user("ketoan", Role.ACCOUNTANT);
    }

    /** Soạn, hoàn tất, xuất kho rồi đánh dấu đã giao; trả về ID phiếu xuất. */
    protected long deliver(long orderId, String pickedA) throws Exception {
        long list = picking.create(List.of(orderId), keeper);
        for (PickingList.Line line : picking.get(list, keeper).getLines()) {
            BigDecimal picked = "A".equals(line.getSku()) ? new BigDecimal(pickedA) : line.getRequired();
            picking.pick(list, line.getId(), picked, picked.compareTo(line.getRequired()) < 0 ? "Thiếu" : null, keeper);
        }
        picking.complete(list, keeper);
        long shipment = exports.export(orderId, keeper);
        sql("UPDATE shipments SET status='DELIVERED', delivered_at=CURRENT_TIMESTAMP, invoice_eligible_at=CURRENT_TIMESTAMP WHERE id=" + shipment);
        return shipment;
    }

    @Test
    void mergesDeliveredShipmentsIntoOneInvoiceAndBooksDebt() throws Exception {
        long s1 = deliver(first, "5");
        long s2 = deliver(second, "3");
        assertEquals(2, invoices.page(accountant).eligible().size());
        assertEquals(2, count("order_credit_reservations", "customer_id=900"));

        long id = invoices.issue(List.of(s2, s1), accountant);

        Invoice invoice = invoices.get(id, accountant);
        assertEquals("HD000001", invoice.getCode());
        // Giá đã chốt: A 10.000 × 5 + B 5.000 × 2 + A 10.000 × 3
        assertEquals(0, new BigDecimal("90000").compareTo(invoice.getTotalAmount()));
        assertEquals(3, invoice.getLines().size());
        assertEquals(LocalDate.now().plusDays(30), invoice.getDueDate());
        assertEquals(Invoice.Status.ISSUED, invoice.getStatus());
        assertEquals(1, count("customer_receivables", "customer_id=900 AND document_code='HD000001' AND outstanding=90000"));
        assertEquals(0, count("order_credit_reservations", "customer_id=900"));
        assertEquals(2, count("shipments", "invoice_id=" + id));
        assertTrue(invoices.page(accountant).eligible().isEmpty());
        assertEquals(1, count("audit_logs", "action='INVOICE_ISSUE'"));
        assertTrue(invoices.pdf(id, accountant).length > 1000);
    }

    @Test
    void invoiceUsesDeliveredQuantityNotOrderedQuantity() throws Exception {
        long shipment = deliver(first, "3");

        Invoice invoice = invoices.get(invoices.issue(List.of(shipment), accountant), accountant);

        // Giao 3/5 hàng A và đủ 2 hàng B: 30.000 + 10.000
        assertEquals(0, new BigDecimal("40000").compareTo(invoice.getTotalAmount()));
    }

    @Test
    void numbersStayContinuousAndGuardsHold() throws Exception {
        long s1 = deliver(first, "5");
        long list = picking.create(List.of(second), keeper);
        for (PickingList.Line line : picking.get(list, keeper).getLines()) {
            picking.pick(list, line.getId(), line.getRequired(), null, keeper);
        }
        picking.complete(list, keeper);
        long notDelivered = exports.export(second, keeper);

        long pending = notDelivered;
        assertThrows(ServiceException.class, () -> invoices.issue(List.of(s1, pending), accountant));
        assertThrows(ForbiddenException.class, () -> invoices.issue(List.of(s1), rep));
        assertThrows(ServiceException.class, () -> invoices.issue(List.of(), accountant));

        long id = invoices.issue(List.of(s1), accountant);
        // Lần lỗi phía trên không tiêu số: hoá đơn đầu tiên vẫn là HD000001
        assertEquals("HD000001", invoices.get(id, accountant).getCode());
        assertThrows(ServiceException.class, () -> invoices.issue(List.of(s1), accountant));

        sql("UPDATE shipments SET status='DELIVERED' WHERE id=" + pending);
        assertEquals("HD000002", invoices.get(invoices.issue(List.of(pending), accountant), accountant).getCode());
    }
}
