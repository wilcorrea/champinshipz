package com.championshipz.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Participant;
import com.championshipz.domain.Team;
import com.championshipz.repository.ChampionshipRepository;
import com.championshipz.repository.TeamRepository;
import com.championshipz.service.ChampionshipService;

@Component
public class DataSeeder implements ApplicationRunner {

    private final ChampionshipRepository championships;
    private final TeamRepository teams;

    public DataSeeder(ChampionshipRepository championships, TeamRepository teams) {
        this.championships = championships;
        this.teams = teams;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (championships.count() > 0) {
            return;
        }

        Team brazil = teams.save(new Team(null, "Brasil", "BRA"));
        Team senegal = teams.save(new Team(null, "Senegal", "SEN"));
        Team czechia = teams.save(new Team(null, "Tchéquia", "CZE"));
        Team uzbekistan = teams.save(new Team(null, "Uzbequistão", "UZB"));

        championships.save(new Championship(
            null, "Copa do Mundo FIFA 2026", "world-cup-2026", ChampionshipType.CUP,
            List.of(
                new Participant(brazil.id(), "B"),
                new Participant(senegal.id(), "B"),
                new Participant(czechia.id(), "B"),
                new Participant(uzbekistan.id(), "B")
            ),
            2,
            ChampionshipService.DEFAULT_STANDINGS_COLUMNS, null));

        Team palmeiras = teams.save(new Team(null, "Palmeiras", "PAL"));
        Team flamengo = teams.save(new Team(null, "Flamengo", "FLA"));
        Team corinthians = teams.save(new Team(null, "Corinthians", "COR"));
        Team santos = teams.save(new Team(null, "Santos", "SAN"));

        championships.save(new Championship(
            null, "Brasileirão 2026", "brasileirao-2026", ChampionshipType.LEAGUE,
            List.of(
                new Participant(palmeiras.id(), null),
                new Participant(flamengo.id(), null),
                new Participant(corinthians.id(), null),
                new Participant(santos.id(), null)
            ),
            null,
            ChampionshipService.DEFAULT_STANDINGS_COLUMNS, null));
    }
}
