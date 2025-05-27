package com.example.api.proxy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class TrackingData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String trackingCode;
    private String lastStatus;
    private String pushToken;
    private LocalDateTime lastUpdated;
}