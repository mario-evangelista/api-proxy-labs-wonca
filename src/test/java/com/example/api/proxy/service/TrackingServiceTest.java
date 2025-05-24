package com.example.api.proxy.service;

import com.example.api.proxy.entity.TrackingData;
import com.example.api.proxy.exception.InvalidTrackingCodeException;
import com.example.api.proxy.repository.TrackingDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackingServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private TrackingDataRepository trackingDataRepository;

    @InjectMocks
    private TrackingService trackingService;

    @Test
    public void testTrackPackageInvalidCode() {
        String invalidCode = "invalid";
        assertThrows(InvalidTrackingCodeException.class, () -> trackingService.trackPackage(invalidCode));
    }

    @Test
    public void testTrackPackageSuccess() {
        String validCode = "AB123456789BR";
        String apiResponse = "{\"json\": \"{\\\"eventos\\\": [{\\\"descricao\\\": \\\"Objeto entregue\\\"}]}\"}";
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(apiResponse, HttpStatus.OK));
        when(trackingDataRepository.findByTrackingCode(validCode)).thenReturn(Optional.empty());

        String result = trackingService.trackPackage(validCode);

        assertEquals(apiResponse, result);
        verify(trackingDataRepository).save(any(TrackingData.class));
    }
}