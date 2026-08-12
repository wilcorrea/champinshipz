package com.championshipz.dto;

import java.util.List;

import com.championshipz.domain.ChampionshipType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateChampionshipRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 60)
    @Pattern(
        regexp = "[a-z0-9]+(-[a-z0-9]+)*",
        message = "must use only lowercase letters, digits and single hyphens")
    String slug,
    @NotNull ChampionshipType type,
    @Positive Integer knockoutSlots,
    List<String> standingsColumns
) {
}
