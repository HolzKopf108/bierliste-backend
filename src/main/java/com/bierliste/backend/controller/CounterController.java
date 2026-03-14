package com.bierliste.backend.controller;

import com.bierliste.backend.dto.CounterIncrementDto;
import com.bierliste.backend.dto.CounterResponseDto;
import com.bierliste.backend.model.Counter;
import com.bierliste.backend.repository.CounterRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/counter")
public class CounterController {

    @Autowired
    private CounterRepository counterRepository;

    @GetMapping
    public ResponseEntity<CounterResponseDto> getCounter() {
        Counter counter = counterRepository.findById(1L)
                .orElseGet(() -> counterRepository.save(new Counter(0)));
        return ResponseEntity.ok(new CounterResponseDto(counter.getCount()));
    }

    @PostMapping
    public ResponseEntity<CounterResponseDto> updateCounter(@Valid @RequestBody CounterIncrementDto dto) {
        int amount = dto.getAmount();
        Counter counter = counterRepository.findById(1L)
            .orElseGet(() -> new Counter());
        counter.setCount(counter.getCount() + amount);
        counterRepository.save(counter);
        return ResponseEntity.ok(new CounterResponseDto(counter.getCount()));
    }
}
