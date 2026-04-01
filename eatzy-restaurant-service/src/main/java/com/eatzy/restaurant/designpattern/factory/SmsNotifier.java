package com.eatzy.restaurant.designpattern.factory;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * Gui thong bao qua SMS (gia lap).
 */
@Component
@Slf4j
public class SmsNotifier implements Notifier {

    @Override
    public void send(String to, String subject, String body) {
        // Gia lap gui SMS (trong thuc te se dung Twilio hoac dich vu SMS)
        log.info("📱 [SMS] Sending to: {} | Message: {}", to, body);
    }

    @Override
    public String getChannel() {
        return "SMS";
    }
}
