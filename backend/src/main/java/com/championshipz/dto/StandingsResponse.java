package com.championshipz.dto;

import java.util.List;

import com.championshipz.domain.ChampionshipType;

public record StandingsResponse(
    ChampionshipType type,
    List<String> columns,
    List<GroupStanding> groups,
    List<StandingRow> table
) {
}
