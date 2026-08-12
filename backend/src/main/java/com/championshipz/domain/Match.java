package com.championshipz.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("matches")
public record Match(
    @Id String id,
    String championship,
    MatchStage stage,
    String group,
    String round,
    Integer matchNumber,
    String homeTeamId,
    String awayTeamId,
    Integer homeGoals,
    Integer awayGoals,
    Integer homePenalties,
    Integer awayPenalties,
    Instant playedAt
) {
}
