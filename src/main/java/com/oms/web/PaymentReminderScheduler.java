package com.oms.web;

import com.oms.service.PaymentReminderService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Tác vụ nền chạy một lần mỗi ngày vào 00:05 theo múi giờ của máy chủ. */
public class PaymentReminderScheduler {

    private static final Logger LOG = Logger.getLogger(PaymentReminderScheduler.class.getName());
    private static final LocalTime RUN_TIME = LocalTime.of(0, 5);
    private static final long PERIOD_HOURS = 24;

    private final PaymentReminderService service;
    private final ScheduledExecutorService executor;

    public PaymentReminderScheduler() {
        this(new PaymentReminderService());
    }

    PaymentReminderScheduler(PaymentReminderService service) {
        this.service = service;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "oms-payment-reminder");
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newSingleThreadScheduledExecutor(factory);
    }

    public void start() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().atTime(RUN_TIME);
        if (!next.isAfter(now)) {
            next = now.toLocalDate().plusDays(1).atTime(RUN_TIME);
        }
        long initialDelay = Math.max(1, Duration.between(now, next).getSeconds());
        executor.scheduleAtFixedRate(this::runSafely, initialDelay, PERIOD_HOURS * 60 * 60, TimeUnit.SECONDS);
        LOG.info("Đã khởi động tác vụ nhắc hạn thanh toán; lịch chạy hằng ngày lúc 00:05.");
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void runSafely() {
        try {
            int count = service.sendDailyReminders(LocalDate.now());
            LOG.info("S7-08: đã xử lý " + count + " hoá đơn cần nhắc hạn.");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "S7-08: tác vụ nhắc hạn thanh toán thất bại", e);
        }
    }
}