package com.oms.web;

import com.oms.config.AppConfig;
import com.oms.service.OrderLifecycleService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** GHTK-54: mỗi giờ tự huỷ đơn đã duyệt giữ chỗ quá order.reservationDays ngày (mặc định 3). */
@WebListener
public class ReservationExpiryListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(ReservationExpiryListener.class.getName());

    private ScheduledExecutorService executor;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        String configured = AppConfig.get("order.reservationDays");
        int days = configured.matches("[1-9][0-9]?") ? Integer.parseInt(configured) : 3;
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "oms-reservation-expiry");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(() -> {
            try {
                int count = new OrderLifecycleService().expireReservations(LocalDateTime.now().minusDays(days));
                if (count > 0) LOG.info("S5-06: đã tự huỷ " + count + " đơn quá hạn giữ chỗ " + days + " ngày.");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "S5-06: tác vụ nhả giữ chỗ quá hạn thất bại", e);
            }
        }, 1, 60, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
