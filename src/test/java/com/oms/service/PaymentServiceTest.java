package com.oms.service;

import com.oms.model.Invoice;
import com.oms.model.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentServiceTest extends InvoiceServiceTest {

    private final PaymentService payments = new PaymentService();

    /** Hai hoá đơn của đại lý 900: cũ hơn 60.000 ₫ (đơn đầu), mới hơn 30.000 ₫ (đơn sau). */
    private long[] twoInvoices() throws Exception {
        long older = invoices.issue(List.of(deliver(first, "5")), accountant);
        long newer = invoices.issue(List.of(deliver(second, "3")), accountant);
        return new long[] {older, newer};
    }

    private PaymentService.Form form(Long invoiceId, String amount) {
        return new PaymentService.Form(900L, invoiceId, new BigDecimal(amount), Payment.Method.CASH, LocalDate.now(),
                accountant.getId(), null);
    }

    @Test
    void autoAllocationPaysOldestInvoiceFirstAndAllowsPartial() throws Exception {
        long[] ids = twoInvoices();

        payments.record(form(null, "70000"), accountant);

        Invoice older = invoices.get(ids[0], accountant);
        Invoice newer = invoices.get(ids[1], accountant);
        assertEquals(Invoice.Status.PAID, older.getStatus());
        assertEquals(Invoice.Status.PARTIAL, newer.getStatus());
        assertEquals(0, new BigDecimal("20000").compareTo(newer.getOutstanding()));
        assertEquals(1, newer.getPayments().size());
        assertEquals("PT000001", newer.getPayments().get(0).getPaymentCode());
        assertEquals(1, count("customer_receivables", "document_code='HD000001' AND outstanding=0"));
        assertEquals(1, count("customer_receivables", "document_code='HD000002' AND outstanding=20000"));
        assertEquals(2, count("payment_allocations", "1=1"));
        assertEquals(1, count("audit_logs", "action='PAYMENT_RECORD'"));
        assertEquals(1, payments.page(accountant).openInvoices().size());
    }

    @Test
    void paymentCanTargetOneInvoice() throws Exception {
        long[] ids = twoInvoices();

        payments.record(form(ids[1], "10000"), accountant);

        assertEquals(Invoice.Status.ISSUED, invoices.get(ids[0], accountant).getStatus());
        Invoice newer = invoices.get(ids[1], accountant);
        assertEquals(Invoice.Status.PARTIAL, newer.getStatus());
        assertEquals(0, new BigDecimal("10000").compareTo(newer.getPaidAmount()));
    }

    @Test
    void rejectsOverpaymentAndBadInputWithoutUsingNumbers() throws Exception {
        long[] ids = twoInvoices();

        assertThrows(ServiceException.class, () -> payments.record(form(null, "90001"), accountant));
        assertThrows(ServiceException.class, () -> payments.record(form(ids[1], "30001"), accountant));
        assertThrows(ServiceException.class, () -> payments.record(form(null, "100.5"), accountant));
        assertThrows(ServiceException.class, () -> payments.record(new PaymentService.Form(900L, null,
                new BigDecimal("1000"), Payment.Method.CASH, LocalDate.now().plusDays(1), accountant.getId(), null), accountant));
        assertThrows(ForbiddenException.class, () -> payments.record(form(null, "1000"), rep));

        payments.record(form(null, "90000"), accountant);
        assertEquals("PT000001", invoices.get(ids[0], accountant).getPayments().get(0).getPaymentCode());
        assertEquals(Invoice.Status.PAID, invoices.get(ids[1], accountant).getStatus());
        // Hết nợ thì không thu thêm được
        assertThrows(ServiceException.class, () -> payments.record(form(null, "1"), accountant));
    }
}
