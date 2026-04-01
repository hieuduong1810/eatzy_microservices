package com.eatzy.common.event;

import java.io.Serializable;

public class UserStatusChangedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String role;
    private Boolean isActive;

    public UserStatusChangedEvent() {
    }

    public UserStatusChangedEvent(Long userId, String role, Boolean isActive) {
        this.userId = userId;
        this.role = role;
        this.isActive = isActive;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "UserStatusChangedEvent{" +
                "userId=" + userId +
                ", role='" + role + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
