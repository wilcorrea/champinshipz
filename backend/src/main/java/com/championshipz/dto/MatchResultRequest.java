package com.championshipz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MatchResultRequest(
    @NotNull @PositiveOrZero Integer homeGoals,
    @NotNull @PositiveOrZero Integer awayGoals,
    @PositiveOrZero Integer homePenalties,
    @PositiveOrZero Integer awayPenalties
) {
}
