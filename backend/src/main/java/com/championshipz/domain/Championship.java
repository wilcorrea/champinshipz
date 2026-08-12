package com.championshipz.domain;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("championships")
public record Championship(
    @Id String id,
    String name,
    @Indexed(unique = true) String slug,
    ChampionshipType type,
    List<Participant> teams,
    Integer knockoutSlots,
    List<String> standingsColumns,
    String ownerId
) {
}
