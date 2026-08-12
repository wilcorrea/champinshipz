package com.championshipz.dto;

import jakarta.validation.constraints.NotBlank;

public record AddParticipantRequest(
    @NotBlank String teamId,
    String group
) {
}
