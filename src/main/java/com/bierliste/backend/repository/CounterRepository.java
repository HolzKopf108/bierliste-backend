package com.bierliste.backend.repository;

import com.bierliste.backend.model.Counter;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterRepository extends JpaRepository<Counter, Long> {
}
