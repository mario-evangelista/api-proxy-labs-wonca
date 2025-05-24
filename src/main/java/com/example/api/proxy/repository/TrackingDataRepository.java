package com.example.api.proxy.repository;

import com.example.api.proxy.entity.TrackingData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackingDataRepository extends JpaRepository<TrackingData, String> {
    Optional<TrackingData> findByTrackingCode(String trackingCode);
}