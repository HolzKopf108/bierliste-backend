package com.bierliste.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Counter {

    @Id
    private Long id = 1L;

    private int count;

    public Counter() {}

    public Counter(int count) {
        this.count = count;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
