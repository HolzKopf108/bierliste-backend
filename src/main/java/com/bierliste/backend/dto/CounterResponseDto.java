package com.bierliste.backend.dto;

public class CounterResponseDto {

    private int count;

    public CounterResponseDto() {
    }

    public CounterResponseDto(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
