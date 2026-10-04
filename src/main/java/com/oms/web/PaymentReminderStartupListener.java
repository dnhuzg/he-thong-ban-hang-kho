package com.oms.web;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Khởi động và dừng scheduler theo vòng đời của ứng dụng Tomcat. */
@WebListener
public class PaymentReminderStartupListener implements ServletContextListener {

    private PaymentReminderScheduler scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler = new PaymentReminderScheduler();
        scheduler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.stop();
        }
    }
}