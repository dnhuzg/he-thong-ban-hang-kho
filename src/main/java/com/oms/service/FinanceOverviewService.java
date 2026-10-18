package com.oms.service;

import com.oms.config.Db;
import com.oms.dao.FinanceOverviewDao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FinanceOverviewService {
    private final FinanceOverviewDao dao=new FinanceOverviewDao();
    public List<FinanceOverviewDao.CreditAlert> creditAlerts() throws SQLException {try(Connection conn=Db.getConnection()){return dao.creditAlerts(conn);}}
    public FinanceOverviewDao.LedgerData ledger() throws SQLException {try(Connection conn=Db.getConnection()){return dao.ledger(conn);}}
}
