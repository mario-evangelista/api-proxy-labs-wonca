package com.example.api.proxy;

import com.example.api.proxy.controller.TrackingController;
import com.example.api.proxy.dto.TrackRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class GlobalExceptionHandlerTest {

    @Autowired
    private TrackingController controller;

    @Test
    public void testValidationException() {
        TrackRequest request = new TrackRequest();
        ResponseEntity<?> response = controller.trackPackage(request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}