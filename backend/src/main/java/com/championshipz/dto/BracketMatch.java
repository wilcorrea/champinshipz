package com.championshipz.dto;

public record BracketMatch(
    String id,
    String round,
    Integer matchNumber,
    BracketTeam home,
    BracketTeam away,
    Integer homeGoals,
    Integer awayGoals,
    Integer homePenalties,
    Integer awayPenalties,
    String winnerTeamId,
    boolean played
) {
}
