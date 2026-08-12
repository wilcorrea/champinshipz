package com.championshipz.dto;

import java.util.List;

public record BracketResponse(
    String championship,
    List<BracketRound> rounds
) {
}
