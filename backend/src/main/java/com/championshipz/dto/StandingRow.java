package com.championshipz.dto;

import java.util.List;

public record StandingRow(
    int position,
    String status,
    String teamId,
    String teamName,
    String teamCode,
    int matchesPlayed,
    int wins,
    int draws,
    int losses,
    int goalsFor,
    int goalsAgainst,
    int goalDifference,
    int points,
    List<String> form
) {
}
