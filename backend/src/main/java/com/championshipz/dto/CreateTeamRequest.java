package com.championshipz.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTeamRequest(
    @NotBlank String name,
    @NotBlank String code
) {
}
