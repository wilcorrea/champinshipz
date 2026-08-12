package com.championshipz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateMatchRequest(
    @NotBlank String homeTeamId,
    @NotBlank String awayTeamId,
    @NotNull @PositiveOrZero Integer homeGoals,
    @NotNull @PositiveOrZero Integer awayGoals
) {
}
