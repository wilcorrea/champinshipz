package com.pifa.pifabackend.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record GrupoResultado(
        @NotBlank @Size(max = 1) String letra,
        @NotBlank List<Map<String, Object>> times
) {
}
