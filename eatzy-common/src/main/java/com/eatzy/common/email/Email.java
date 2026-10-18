package com.eatzy.common.email;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO representing an email message.
 * Created by EmailFactory using the Builder Pattern.
 */
@Getter
@Builder
public class Email {
    private final String to;
    private final String subject;
    private final String htmlBody;
}
