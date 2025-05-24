package com.example.api.proxy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TrackingData {
    @Id
    private String trackingCode;
    private String lastStatus;
    private String pushToken;
    private LocalDateTime lastUpdated;

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public String getPushToken() { return pushToken; }
    public void setPushToken(String pushToken) { this.pushToken = pushToken; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}