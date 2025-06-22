package com.bierliste.backend.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/ping")
public class PingController {
    
    @GetMapping
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
