package com.oms.web;

import com.oms.service.UserService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class AppStartupListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppStartupListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            new UserService().ensureInitialAdmin();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Không khởi tạo được dữ liệu ban đầu — kiểm tra kết nối MySQL trong app.properties", e);
        }
    }
}
