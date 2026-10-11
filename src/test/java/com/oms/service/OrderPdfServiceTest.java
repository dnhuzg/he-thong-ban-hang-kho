package com.oms.service;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.oms.model.OrderLine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class OrderPdfServiceTest extends OrderWorkflowServiceTest {
    @Test void vietnamesePdfHasCustomerLinesTotalsAndOrderCode() throws Exception {
        workflow.submit(id,rep);
        byte[] pdf=new OrderPdfService().export(id,rep);
        var reader=new PdfReader(pdf);
        try {
            String text=new PdfTextExtractor(reader).getTextFromPage(1);
            assertTrue(text.contains("ĐƠN HÀNG")); assertTrue(text.contains("Đại lý"));
            assertTrue(text.contains("Hàng")); assertTrue(text.contains("DH000001"));
            assertTrue(text.contains("TỔNG TIỀN")); assertTrue(text.contains("50.000"));
            assertEquals(1,reader.getNumberOfPages());
        } finally { reader.close(); }
        assertThrows(OrderService.NotFoundException.class,()->new OrderPdfService().export(id,other));
        Files.write(Path.of("target/s4-order-example.pdf"),pdf);
    }
    @Test void longOrderBreaksAcrossPagesAndRepeatsTableHeader() throws Exception {
        var order=orders.get(id,rep); var lines=new ArrayList<OrderLine>();
        for(int i=0;i<100;i++) {
            OrderLine line=new OrderLine(); line.setSku("SKU"+i); line.setProductName("Sản phẩm tiếng Việt có tên dài để kiểm tra bảng nhiều trang "+i);
            line.setUnitName("Thùng"); line.setQuantity(1); line.setUnitPrice(BigDecimal.TEN); line.setGrossAmount(BigDecimal.TEN); line.setDiscountAmount(BigDecimal.ZERO); lines.add(line);
        }
        order.setLines(lines); order.setGrossTotal(new BigDecimal("1000"));
        var reader=new PdfReader(new OrderPdfService().render(order));
        try {
            assertTrue(reader.getNumberOfPages()>1);
            assertTrue(new PdfTextExtractor(reader).getTextFromPage(2).contains("Thành tiền"));
        } finally { reader.close(); }
    }
}
