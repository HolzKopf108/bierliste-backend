package com.bierliste.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateGroupDto {

    @NotBlank(message = "Gruppenname ist erforderlich")
    @Size(min = 3, max = 120, message = "Gruppenname muss zwischen 3 und 120 Zeichen lang sein")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}