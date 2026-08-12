package com.championshipz.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Match;
import com.championshipz.domain.MatchStage;
import com.championshipz.domain.Participant;
import com.championshipz.dto.CreateMatchRequest;
import com.championshipz.dto.MatchResultRequest;
import com.championshipz.repository.MatchRepository;
import com.championshipz.web.NotFoundException;

@Service
public class MatchService {

    private final ChampionshipService championships;
    private final MatchRepository matches;
    private final BracketService bracket;

    public MatchService(ChampionshipService championships, MatchRepository matches, BracketService bracket) {
        this.championships = championships;
        this.matches = matches;
        this.bracket = bracket;
    }

    public Match register(String slug, CreateMatchRequest req, String ownerId) {
        Championship championship = championships.requireOwned(slug, ownerId);

        Participant home = participantOf(championship, req.homeTeamId());
        Participant away = participantOf(championship, req.awayTeamId());

        if (home.teamId().equals(away.teamId())) {
            throw new IllegalArgumentException("A team cannot play against itself");
        }

        MatchStage stage;
        String group;
        if (championship.type() == ChampionshipType.LEAGUE) {
            stage = MatchStage.LEAGUE;
            group = null;
        } else {
            stage = MatchStage.GROUP;
            if (home.group() == null || !home.group().equals(away.group())) {
                throw new IllegalArgumentException("Both teams must be in the same group");
            }
            group = home.group();
        }

        Match match = new Match(
            null, slug, stage, group, null, null,
            req.homeTeamId(), req.awayTeamId(),
            req.homeGoals(), req.awayGoals(), null, null,
            Instant.now());
        Match saved = matches.save(match);

        bracket.materializeIfComplete(slug);
        return saved;
    }

    public Match setResult(String slug, String matchId, MatchResultRequest req, String ownerId) {
        championships.requireOwned(slug, ownerId);

        Match match = matches.findById(matchId)
            .orElseThrow(() -> new NotFoundException("Match '" + matchId + "' not found"));
        if (!match.championship().equals(slug)) {
            throw new IllegalArgumentException("Match does not belong to championship '" + slug + "'");
        }
        if (match.homeTeamId() == null || match.awayTeamId() == null) {
            throw new IllegalArgumentException("This bracket slot has no teams yet (waiting for previous rounds)");
        }

        Match updated = new Match(
            match.id(), match.championship(), match.stage(), match.group(),
            match.round(), match.matchNumber(),
            match.homeTeamId(), match.awayTeamId(),
            req.homeGoals(), req.awayGoals(),
            req.homePenalties(), req.awayPenalties(),
            Instant.now());
        Match saved = matches.save(updated);

        if (saved.stage() == MatchStage.KNOCKOUT) {
            bracket.advance(saved);
        }
        return saved;
    }

    private Participant participantOf(Championship championship, String teamId) {
        return championship.teams().stream()
            .filter(p -> p.teamId().equals(teamId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Team '" + teamId + "' is not registered in this championship"));
    }
}
