package com.eatzy.restaurant.designpattern.factory;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Gui thong bao qua Email (gia lap).
 */
@Component
@Slf4j
public class EmailNotifier implements Notifier {

    @Override
    public void send(String to, String subject, String body) {
        // Gia lap gui email (trong thuc te se dung JavaMailSender)
        log.info("📧 [EMAIL] Sending to: {} | Subject: {} | Body: {}", to, subject, body);
    }

    @Override
    public String getChannel() {
        return "EMAIL";
    }
}
