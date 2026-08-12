package com.championshipz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateChampionshipRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 60)
    @Pattern(
        regexp = "[a-z0-9]+(-[a-z0-9]+)*",
        message = "must use only lowercase letters, digits and single hyphens")
    String slug
) {
}
