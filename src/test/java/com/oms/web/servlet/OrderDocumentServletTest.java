package com.oms.web.servlet;

import com.oms.model.Role;
import com.oms.service.OrderService;
import com.oms.support.DatabaseTest;
import com.oms.support.HttpFixture;
import com.oms.web.SessionKeys;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderDocumentServletTest extends DatabaseTest {
    @Test void pagePdfSupportsPreviewAndDownloadAndHidesForeignOrders() throws Exception {
        var owner=user("owner",Role.SALES_REP);
        var foreign=user("foreign",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id) VALUES(900,'DL','Đại lý Việt',1,"+owner.getId()+")");
        long id=new OrderService().create(900,owner);
        var http=new HttpFixture(); http.path="/orders/pdf";
        http.parameters.put("id",Long.toString(id)); http.attributes.put(SessionKeys.CURRENT_USER,owner);
        var servlet=new OrderPdfServlet();
        servlet.doGet(http.request,http.response);
        assertEquals("application/pdf",http.contentType);
        assertTrue(http.headers.get("Content-Disposition").startsWith("inline;"));
        assertEquals("no-store",http.headers.get("Cache-Control"));
        assertTrue(http.binaryBody.toString(java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"));
        http.parameters.put("download","true"); servlet.doGet(http.request,http.response);
        assertTrue(http.headers.get("Content-Disposition").startsWith("attachment;"));
        http.binaryBody.reset(); http.attributes.put(SessionKeys.CURRENT_USER,foreign);
        servlet.doGet(http.request,http.response);
        assertEquals(404,http.status); assertEquals(0,http.binaryBody.size());
    }
}
