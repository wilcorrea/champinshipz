package com.championshipz.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("teams")
public record Team(
    @Id String id,
    String name,
    String code
) {
}
