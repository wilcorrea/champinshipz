package com.championshipz.service;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Match;
import com.championshipz.domain.Participant;
import com.championshipz.dto.AddParticipantRequest;
import com.championshipz.dto.CreateChampionshipRequest;
import com.championshipz.dto.UpdateChampionshipRequest;
import com.championshipz.repository.ChampionshipRepository;
import com.championshipz.repository.MatchRepository;
import com.championshipz.repository.TeamRepository;
import com.championshipz.web.ForbiddenException;
import com.championshipz.web.NotFoundException;

@Service
public class ChampionshipService {

    public static final List<String> DEFAULT_STANDINGS_COLUMNS = List.of(
            "matchesPlayed", "wins", "draws", "losses",
            "goalsFor", "goalsAgainst", "goalDifference", "points", "form"
    );

    private final ChampionshipRepository championships;
    private final TeamRepository teams;
    private final MatchRepository matches;

    public ChampionshipService(ChampionshipRepository championships, TeamRepository teams,
                               MatchRepository matches) {
        this.championships = championships;
        this.teams = teams;
        this.matches = matches;
    }

    public Championship create(CreateChampionshipRequest req, String ownerId) {
        if (championships.existsBySlug(req.slug())) {
            throw new IllegalArgumentException("A championship with slug '" + req.slug() + "' already exists");
        }
        if (req.type() == ChampionshipType.CUP && req.knockoutSlots() != null
                && (req.knockoutSlots() < 2 || Integer.bitCount(req.knockoutSlots()) != 1)) {
            throw new IllegalArgumentException("knockoutSlots must be a power of two (2, 4, 8, 16, 32...)");
        }
        List<String> columns = (req.standingsColumns() == null || req.standingsColumns().isEmpty())
                ? DEFAULT_STANDINGS_COLUMNS
                : req.standingsColumns();
        return championships.save(new Championship(
                null, req.name(), req.slug(), req.type(), new ArrayList<>(),
                req.knockoutSlots(), columns, ownerId));
    }

    public List<Championship> list() {
        return championships.findAll();
    }

    public Championship getBySlug(String slug) {
        return championships.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Championship '" + slug + "' not found"));
    }

    public void assertOwner(@NonNull Championship championship, String ownerId) {
        if (championship.ownerId() == null) {
            throw new ForbiddenException(
                    "'" + championship.slug() + "' is a public demo championship and cannot be changed");
        }
        if (!championship.ownerId().equals(ownerId)) {
            throw new ForbiddenException("Only the owner can change '" + championship.slug() + "'");
        }
    }

    public Championship requireOwned(String slug, String ownerId) {
        Championship championship = getBySlug(slug);
        assertOwner(championship, ownerId);
        return championship;
    }

    public Championship addParticipant(String slug, @NonNull AddParticipantRequest req, String ownerId) {
        Championship championship = requireOwned(slug, ownerId);

        if (!teams.existsById(req.teamId())) {
            throw new IllegalArgumentException("Team '" + req.teamId() + "' not found");
        }
        boolean already = championship.teams().stream()
                .anyMatch(p -> p.teamId().equals(req.teamId()));
        if (already) {
            throw new IllegalArgumentException("Team '" + req.teamId() + "' is already registered in this championship");
        }
        if (championship.type() == ChampionshipType.CUP && (req.group() == null || req.group().isBlank())) {
            throw new IllegalArgumentException("Group is required for a cup championship");
        }

        List<Participant> updated = new ArrayList<>(championship.teams());
        updated.add(new Participant(req.teamId(), req.group()));

        return championships.save(new Championship(
                championship.id(), championship.name(), championship.slug(),
                championship.type(), updated, championship.knockoutSlots(),
                championship.standingsColumns(), championship.ownerId()));
    }

    public List<Participant> participants(String slug) {
        return getBySlug(slug).teams();
    }

    public Championship update(String slug, @NonNull UpdateChampionshipRequest req, String ownerId) {
        Championship championship = requireOwned(slug, ownerId);

        String newSlug = req.slug().trim();
        boolean slugChanged = !newSlug.equals(slug);

        if (slugChanged && championships.existsBySlug(newSlug)) {
            throw new IllegalArgumentException("A championship with slug '" + newSlug + "' already exists");
        }

        Championship saved = championships.save(new Championship(
                championship.id(), req.name().trim(), newSlug, championship.type(),
                championship.teams(), championship.knockoutSlots(),
                championship.standingsColumns(), championship.ownerId()));

        if (slugChanged) {
            repointMatches(slug, newSlug);
        }
        return saved;
    }

    /**
     * {@link Match#championship()} guarda a slug, não o ‘id’ do campeonato. Renomear a slug sem
     * mexer nos jogos deixaria todos órfãos: a classificação zeraria e o chaveamento sumiria.
     */
    private void repointMatches(String oldSlug, String newSlug) {
        List<Match> moved = matches.findByChampionshipOrderByPlayedAtAsc(oldSlug).stream()
                .map(match -> new Match(
                        match.id(), newSlug, match.stage(), match.group(),
                        match.round(), match.matchNumber(),
                        match.homeTeamId(), match.awayTeamId(),
                        match.homeGoals(), match.awayGoals(),
                        match.homePenalties(), match.awayPenalties(),
                        match.playedAt()))
                .toList();
        matches.saveAll(moved);
    }

    public void delete(String slug, String ownerId) {
        Championship championship = requireOwned(slug, ownerId);
        matches.deleteByChampionship(slug);
        championships.delete(championship);
    }
}
