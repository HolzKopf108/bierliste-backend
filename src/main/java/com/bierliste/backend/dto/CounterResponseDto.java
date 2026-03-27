package com.bierliste.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class CounterResponseDto {

    @Schema(description = "Aktueller Strichsaldo. Positiv bedeutet Schulden, negativ bedeutet Guthaben.")
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
