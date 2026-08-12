package com.championshipz.dto;

import java.util.List;

public record BracketRound(
    String round,
    List<BracketMatch> matches
) {
}
