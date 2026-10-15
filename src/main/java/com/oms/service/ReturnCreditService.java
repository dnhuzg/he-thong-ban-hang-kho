package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.AuditLogDao;
import com.oms.dao.ReturnCreditDao;
import com.oms.model.Role;
import com.oms.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** S8-02: ghi chứng từ giảm trừ; hóa đơn gốc không bị sửa. */
public class ReturnCreditService {
    public record PageData(List<ReturnCreditDao.Eligible> eligible,List<ReturnCreditDao.Document> documents) { }
    public record Saved(long id,String code) { }
    private final ReturnCreditDao dao=new ReturnCreditDao();private final AuditLogDao audit=new AuditLogDao();
    public PageData page() throws SQLException {try(Connection conn=Db.getConnection()){return new PageData(dao.eligible(conn),dao.recent(conn,50));}}
    public Saved create(long returnId,User actor) throws SQLException {
        if(!(actor.hasRole(Role.ACCOUNTANT)||actor.isAdmin()))throw new ForbiddenException("Chỉ Kế toán công nợ hoặc Admin được lập chứng từ.");
        return Db.inTransaction(conn->{ReturnCreditDao.Eligible item=dao.lockEligible(conn,returnId);if(item==null)throw new ServiceException("Phiếu trả chưa được nhận hoặc đã có chứng từ giảm trừ.");if(item.amount()==null||item.amount().signum()<=0)throw new ServiceException("Giá trị trả hàng không hợp lệ.");if(item.amount().compareTo(item.debt())>0)throw new ServiceException("Giá trị giảm vượt công nợ hiện tại của đại lý.");String code=String.format("GT%06d",dao.nextNumber(conn));long id=dao.insert(conn,code,item,actor.getId());dao.applyToInvoice(conn,item.invoiceId(),item.amount());dao.reduceDebt(conn,item.customerId(),item.amount());audit.log(conn,actor.getId(),"RETURN_CREDIT_CREATE","RETURN_CREDIT",id,code+" · phiếu "+item.returnCode()+" · "+item.amount());return new Saved(id,code);});
    }
}
