package com.oms.web.servlet;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.support.DatabaseTest;
import com.oms.support.HttpFixture;
import com.oms.web.SessionKeys;
import com.oms.web.filter.CsrfFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderBackendServletTest extends DatabaseTest {
    @Test void portalCreateIgnoresClientCustomerIdAndReturnsJson201() throws Exception {
        User customer=user("customer",Role.CUSTOMER);
        sql("INSERT INTO customers(id,code,name,group_id) VALUES(900,'DL','Đại lý',1)");
        sql("INSERT INTO customer_accounts VALUES(900,"+customer.getId()+")");
        var http=new HttpFixture(); http.path="/api/portal/create"; http.method="POST";
        http.parameters.put("customerId","999"); http.attributes.put(SessionKeys.CURRENT_USER,customer);
        new PortalOrderServlet().doPost(http.request,http.response);
        assertEquals(201,http.status); assertEquals("application/json;charset=UTF-8",http.contentType);
        assertTrue(http.body.toString().contains("\"id\"")); assertEquals(1,count("orders","customer_id=900"));
    }
    @Test void creditEndpointHidesForeignOrderAndUses404() throws Exception {
        User first=user("rep",Role.SALES_REP), second=user("other",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id) VALUES(900,'DL','Đại lý',1,"+first.getId()+")");
        long id=new com.oms.service.OrderService().create(900,first);
        var http=new HttpFixture(); http.path="/api/orders/credit"; http.parameters.put("id",Long.toString(id));
        http.attributes.put(SessionKeys.CURRENT_USER,second);
        new OrderCreditServlet().doGet(http.request,http.response);
        assertEquals(404,http.status); assertTrue(http.body.toString().contains("Không tìm thấy"));
        assertFalse(http.body.toString().contains("currentDebt"));
    }
    @Test void portalPostRequiresSessionCsrfToken() throws Exception {
        var http=new HttpFixture(); http.path="/api/portal/create"; http.method="POST";
        http.sessionAttributes.put(SessionKeys.USER_ID,1L); http.sessionAttributes.put(SessionKeys.CSRF_TOKEN,"valid-token");
        http.parameters.put("_csrf","wrong-token"); boolean[] called={false};
        new CsrfFilter().doFilter(http.request,http.response,(req,resp)->called[0]=true);
        assertEquals(403,http.status); assertFalse(called[0]);
        http.parameters.put("_csrf","valid-token"); http.status=200;
        new CsrfFilter().doFilter(http.request,http.response,(req,resp)->called[0]=true);
        assertTrue(called[0]);
    }
    @Test void copyFromPageRedirectsToNewDraftAndDoesNotExposeForeignSource() throws Exception {
        User rep=user("sales",Role.SALES_REP), foreign=user("foreign",Role.SALES_REP);
        sql("INSERT INTO customers(id,code,name,group_id,sales_rep_id) VALUES(900,'DL','Đại lý',1,"+rep.getId()+")");
        long source=new com.oms.service.OrderService().create(900,rep);
        var http=new HttpFixture(); http.path="/orders/copy"; http.method="POST";
        http.parameters.put("id",Long.toString(source)); http.attributes.put(SessionKeys.CURRENT_USER,rep);
        new OrderCopyServlet().doPost(http.request,http.response);
        assertEquals(302,http.status);
        assertTrue(http.redirect.startsWith("/oms/orders/edit?id="));
        assertNotEquals("/oms/orders/edit?id="+source,http.redirect);
        assertEquals(2,count("orders","1=1"));
        http.attributes.put(SessionKeys.CURRENT_USER,foreign); http.status=200;
        new OrderCopyServlet().doPost(http.request,http.response);
        assertEquals(404,http.status);
        assertEquals(2,count("orders","1=1"));
    }
}
