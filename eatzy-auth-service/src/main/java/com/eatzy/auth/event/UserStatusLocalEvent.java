package com.eatzy.auth.event;

import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class UserStatusLocalEvent extends ApplicationEvent {
    
    private final Long userId;
    private final String role;
    private final Boolean isActive;

    public UserStatusLocalEvent(Object source, Long userId, String role, Boolean isActive) {
        super(source);
        this.userId = userId;
        this.role = role;
        this.isActive = isActive;
    }
}
