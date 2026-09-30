package com.oms.service;

import com.oms.config.AppConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gửi email qua SMTP (cấu hình mail.* trong app.properties).
 * Chưa điền đủ mail.smtp.host / mail.username / mail.password thì chỉ ghi nội dung ra log Tomcat.
 * Gửi lỗi thì người dùng vẫn tự lấy lại quyền truy cập được qua "Quên mật khẩu".
 */
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    /** @return true nếu đã gửi thật qua SMTP; false nếu chỉ ghi log (chưa cấu hình) hoặc gửi lỗi. */
    public boolean send(String to, String subject, String body) {
        String host = AppConfig.get("mail.smtp.host");
        if (host.isEmpty() || AppConfig.get("mail.username").isEmpty() || AppConfig.get("mail.password").isEmpty()) {
            LOG.info("[EMAIL] (chưa cấu hình SMTP, chỉ ghi log) To: " + to + "\nSubject: " + subject + "\n" + body);
            return false;
        }
        String from = AppConfig.get("mail.from").isEmpty() ? AppConfig.get("mail.username") : AppConfig.get("mail.from");
        try {
            MimeMessage message = new MimeMessage(session(host));
            message.setFrom(new InternetAddress(from, AppConfig.get("mail.fromName"), "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            Transport.send(message);
            LOG.info("[EMAIL] Đã gửi \"" + subject + "\" tới " + to);
            return true;
        } catch (MessagingException | UnsupportedEncodingException e) {
            // Không làm hỏng thao tác của người dùng (tài khoản đã tạo, token đã lưu); quản trị xem log để xử lý
            LOG.log(Level.SEVERE, "[EMAIL] Gửi thất bại tới " + to + " — kiểm tra cấu hình mail.* trong app.properties", e);
            return false;
        }
    }

    private static Session session(String host) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", AppConfig.get("mail.smtp.port"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        String username = AppConfig.get("mail.username");
        String password = AppConfig.get("mail.password");
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
}
