package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.SalesReturnDao;
import com.oms.model.NamedItem;
import com.oms.model.Role;
import com.oms.model.User;
import com.oms.security.PermissionPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** S8-01: lập và xác nhận phiếu trả. Tồn kho chỉ đổi trong transaction xác nhận. */
public class SalesReturnService {
    public record LineInput(long invoiceLineId, BigDecimal quantity, String reason, String condition) { }
    public record PageData(List<SalesReturnDao.InvoiceOption> invoices, List<SalesReturnDao.ReturnRow> returns) { }
    public record Saved(long id, String code, String status) { }

    private final SalesReturnDao dao = new SalesReturnDao();
    private final AuditLogDao audit = new AuditLogDao();

    public PageData page() throws SQLException {
        try(Connection conn=Db.getConnection()){return new PageData(dao.eligibleInvoices(conn),dao.recent(conn,50));}
    }

    public Saved create(long invoiceId,List<LineInput> inputs,User actor) throws SQLException {
        if(!PermissionPolicy.canTakeOrders(actor.getRoles())) throw new ForbiddenException("Bạn không có quyền lập phiếu trả hàng.");
        if(invoiceId<=0||inputs==null||inputs.isEmpty()) throw new ServiceException("Chọn hóa đơn và ít nhất một dòng hàng trả.");
        if(inputs.size()>100) throw new ServiceException("Một phiếu tối đa 100 dòng hàng.");
        return Db.inTransaction(conn->{
            SalesReturnDao.LockedInvoice invoice=dao.lockInvoice(conn,invoiceId);
            if(invoice==null) throw new ServiceException("Hóa đơn không tồn tại hoặc đã hủy.");
            Set<Long> ids=new HashSet<>();
            for(LineInput input:inputs) validate(conn,invoice,input,ids);
            long number=dao.nextNumber(conn,"SALES_RETURN");String code=String.format("TH%06d",number);
            long id=dao.insertReturn(conn,code,invoice,actor.getId());
            for(LineInput input:inputs){SalesReturnDao.LockedLine line=dao.lockLine(conn,invoiceId,input.invoiceLineId());dao.insertLine(conn,id,line,input.quantity(),input.reason().strip(),input.condition());}
            audit.log(conn,actor.getId(),"SALES_RETURN_CREATE","SALES_RETURN",id,code+" · hóa đơn "+invoice.code()+" · "+inputs.size()+" dòng");
            return new Saved(id,code,"PENDING");
        });
    }

    public Saved confirm(long returnId,User actor) throws SQLException {
        if(!(actor.hasRole(Role.WH_MANAGER)||actor.isAdmin())) throw new ForbiddenException("Chỉ Quản lý kho hoặc Admin được xác nhận nhận hàng.");
        return Db.inTransaction(conn->{
            SalesReturnDao.ReturnRow row=dao.lockReturn(conn,returnId);
            if(row==null) throw new ServiceException("Không tìm thấy phiếu trả hàng.");
            if(!"PENDING".equals(row.status())) throw new ServiceException("Phiếu trả hàng đã được xác nhận.");
            Set<Long> scope=actor.isAdmin()?null:actor.getWarehouses().stream().map(NamedItem::getId).collect(Collectors.toSet());
            if(scope!=null&&!scope.contains(row.warehouseId())) throw new ForbiddenException("Phiếu không thuộc kho bạn phụ trách.");
            dao.receive(conn,row,actor.getId());
            audit.log(conn,actor.getId(),"SALES_RETURN_RECEIVE","SALES_RETURN",row.id(),row.code()+" · nhập lại kho "+row.warehouse()+" · "+row.lines().size()+" dòng");
            return new Saved(row.id(),row.code(),"RECEIVED");
        });
    }

    private void validate(Connection conn,SalesReturnDao.LockedInvoice invoice,LineInput input,Set<Long> ids) throws SQLException {
        if(input==null||input.invoiceLineId()<=0||!ids.add(input.invoiceLineId())) throw new ServiceException("Dòng hàng trả bị trùng hoặc không hợp lệ.");
        SalesReturnDao.LockedLine line=dao.lockLine(conn,invoice.id(),input.invoiceLineId());
        if(line==null) throw new ServiceException("Dòng hóa đơn không tồn tại.");
        BigDecimal qty=input.quantity();
        if(qty==null||qty.signum()<=0||qty.stripTrailingZeros().scale()>4) throw new ServiceException("Số lượng trả không hợp lệ.");
        BigDecimal remaining=line.quantity().subtract(dao.returnedQuantity(conn,line.id()));
        if(qty.compareTo(remaining)>0) throw new ServiceException("Số lượng trả vượt quá số còn được phép trả.");
        String reason=input.reason()==null?"":input.reason().strip();
        if(reason.isEmpty()||reason.length()>500) throw new ServiceException("Lý do trả phải từ 1 đến 500 ký tự.");
        if(!Set.of("GOOD","DAMAGED").contains(input.condition())) throw new ServiceException("Tình trạng hàng không hợp lệ.");
    }
}
