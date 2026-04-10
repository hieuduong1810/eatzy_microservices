package com.eatzy.common.event;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Event published when a driver goes online.
 * Order service consumes this to assign waiting orders to the newly available driver.
 * Matches eatzy_backend: goOnline triggers search for unassigned PREPARING orders.
 */
public class DriverOnlineEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long driverId;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public DriverOnlineEvent() {
    }

    public DriverOnlineEvent(Long driverId, BigDecimal latitude, BigDecimal longitude) {
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "DriverOnlineEvent{" +
                "driverId=" + driverId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
