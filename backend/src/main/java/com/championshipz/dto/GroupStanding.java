package com.championshipz.dto;

import java.util.List;

public record GroupStanding(
    String name,
    List<StandingRow> rows
) {
}
