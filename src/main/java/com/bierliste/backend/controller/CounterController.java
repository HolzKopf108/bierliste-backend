package com.bierliste.backend.controller;

import com.bierliste.backend.dto.CounterUpdateDto;
import com.bierliste.backend.model.Counter;
import com.bierliste.backend.repository.CounterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/counter")
public class CounterController {

    @Autowired
    private CounterRepository counterRepository;

    @GetMapping
    public ResponseEntity<?> getCounter() {
        Counter counter = counterRepository.findById(1L)
                .orElseGet(() -> counterRepository.save(new Counter(0)));
        return ResponseEntity.ok(Map.of("count", counter.getCount()));
    }

    @PostMapping
    public ResponseEntity<?> updateCounter(@RequestBody CounterUpdateDto dto) {
        int count = dto.getCount();
        Counter counter = counterRepository.findById(1L)
            .orElseGet(() -> new Counter());
        counter.setCount(counter.getCount() + count);
        counterRepository.save(counter);
        return ResponseEntity.ok().build();
    }
}
