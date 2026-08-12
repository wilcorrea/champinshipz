package com.championshipz.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Match;
import com.championshipz.domain.MatchStage;
import com.championshipz.domain.Participant;
import com.championshipz.domain.Team;
import com.championshipz.dto.BracketMatch;
import com.championshipz.dto.BracketResponse;
import com.championshipz.dto.BracketRound;
import com.championshipz.dto.BracketTeam;
import com.championshipz.dto.StandingRow;
import com.championshipz.dto.StandingsResponse;
import com.championshipz.repository.MatchRepository;
import com.championshipz.repository.TeamRepository;

@Service
public class BracketService {

    private static final String FINAL = "FINAL";
    private static final String SEMI_FINAL = "SEMI_FINAL";
    private static final String THIRD_PLACE = "THIRD_PLACE";

    private final ChampionshipService championships;
    private final StandingsService standings;
    private final TeamRepository teams;
    private final MatchRepository matches;

    public BracketService(ChampionshipService championships, StandingsService standings,
                          TeamRepository teams, MatchRepository matches) {
        this.championships = championships;
        this.standings = standings;
        this.teams = teams;
        this.matches = matches;
    }

    public void materializeIfComplete(String slug) {
        Championship championship = championships.getBySlug(slug);
        if (championship.type() != ChampionshipType.CUP || championship.knockoutSlots() == null) {
            return;
        }
        if (matches.existsByChampionshipAndStage(slug, MatchStage.KNOCKOUT)) {
            return;
        }
        if (!groupStageComplete(championship)) {
            return;
        }
        generateBracket(championship);
    }

    private boolean groupStageComplete(Championship championship) {
        Map<String, Long> teamsPerGroup = championship.teams().stream()
            .filter(p -> p.group() != null)
            .collect(Collectors.groupingBy(Participant::group, Collectors.counting()));

        long expected = 0;
        for (long teamsInGroup : teamsPerGroup.values()) {
            expected += teamsInGroup * (teamsInGroup - 1) / 2;
        }
        if (expected == 0) {
            return false;
        }
        long played = matches.findByChampionshipAndStage(championship.slug(), MatchStage.GROUP).stream()
            .filter(m -> m.homeGoals() != null && m.awayGoals() != null)
            .count();
        return played >= expected;
    }

    private void generateBracket(Championship championship) {
        StandingsResponse table = standings.compute(championship.slug());

        List<StandingRow> qualifiers = table.groups().stream()
            .flatMap(group -> group.rows().stream())
            .filter(row -> "QUALIFIED".equals(row.status()))
            .sorted(Comparator
                .comparingInt(StandingRow::position)
                .thenComparing(Comparator.comparingInt(StandingRow::points).reversed())
                .thenComparing(Comparator.comparingInt(StandingRow::goalDifference).reversed())
                .thenComparing(Comparator.comparingInt(StandingRow::goalsFor).reversed()))
            .toList();

        int slots = qualifiers.size();
        if (slots < 2 || Integer.bitCount(slots) != 1) {
            throw new IllegalArgumentException(
                "Knockout needs a power-of-two number of qualified teams, got " + slots);
        }

        int[] order = seedOrder(slots);
        List<Match> bracket = new ArrayList<>();

        String firstRound = roundLabel(slots);
        for (int i = 0; i < slots / 2; i++) {
            String home = qualifiers.get(order[2 * i] - 1).teamId();
            String away = qualifiers.get(order[2 * i + 1] - 1).teamId();
            bracket.add(emptyKnockout(championship.slug(), firstRound, i + 1, home, away));
        }

        for (int size = slots / 2; size >= 2; size /= 2) {
            String round = roundLabel(size);
            for (int number = 1; number <= size / 2; number++) {
                bracket.add(emptyKnockout(championship.slug(), round, number, null, null));
            }
        }

        if (slots >= 4) {
            bracket.add(emptyKnockout(championship.slug(), THIRD_PLACE, 1, null, null));
        }

        matches.saveAll(bracket);
    }

    private Match emptyKnockout(String slug, String round, int matchNumber, String home, String away) {
        return new Match(null, slug, MatchStage.KNOCKOUT, null, round, matchNumber,
            home, away, null, null, null, null, null);
    }

    public void advance(Match played) {
        if (played.stage() != MatchStage.KNOCKOUT) {
            return;
        }
        if (FINAL.equals(played.round()) || THIRD_PLACE.equals(played.round())) {
            return;
        }

        String winner = winnerOf(played);
        String loser = winner.equals(played.homeTeamId()) ? played.awayTeamId() : played.homeTeamId();

        int size = roundSize(played.round());
        String nextRound = roundLabel(size / 2);
        int nextNumber = (played.matchNumber() + 1) / 2;
        boolean homeSide = played.matchNumber() % 2 == 1;

        placeTeam(played.championship(), nextRound, nextNumber, homeSide, winner);
        if (SEMI_FINAL.equals(played.round())) {
            placeTeam(played.championship(), THIRD_PLACE, 1, homeSide, loser);
        }
    }

    private void placeTeam(String slug, String round, int matchNumber, boolean homeSide, String teamId) {
        Match target = matches.findByChampionshipAndRoundAndMatchNumber(slug, round, matchNumber)
            .orElseThrow(() -> new IllegalStateException("Bracket slot " + round + " #" + matchNumber + " is missing"));
        Match updated = new Match(
            target.id(), target.championship(), target.stage(), target.group(),
            target.round(), target.matchNumber(),
            homeSide ? teamId : target.homeTeamId(),
            homeSide ? target.awayTeamId() : teamId,
            target.homeGoals(), target.awayGoals(),
            target.homePenalties(), target.awayPenalties(),
            target.playedAt());
        matches.save(updated);
    }

    public String winnerOf(Match m) {
        if (m.homeGoals() == null || m.awayGoals() == null) {
            throw new IllegalArgumentException("Match has no result yet");
        }
        if (m.homeGoals() > m.awayGoals()) {
            return m.homeTeamId();
        }
        if (m.awayGoals() > m.homeGoals()) {
            return m.awayTeamId();
        }
        if (m.homePenalties() == null || m.awayPenalties() == null) {
            throw new IllegalArgumentException(
                "A knockout tie must be decided on penalties (send homePenalties and awayPenalties)");
        }
        if (m.homePenalties() > m.awayPenalties()) {
            return m.homeTeamId();
        }
        if (m.awayPenalties() > m.homePenalties()) {
            return m.awayTeamId();
        }
        throw new IllegalArgumentException("Penalties are also tied — there must be a winner");
    }

    public BracketResponse getBracket(String slug) {
        championships.getBySlug(slug);
        List<Match> knockout = matches.findByChampionshipAndStage(slug, MatchStage.KNOCKOUT);
        Map<String, Team> teamsById = loadTeams(knockout);

        Map<String, List<Match>> byRound = knockout.stream()
            .collect(Collectors.groupingBy(Match::round));

        List<BracketRound> rounds = byRound.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, List<Match>> entry) -> displayRank(entry.getKey())).reversed())
            .map(entry -> new BracketRound(entry.getKey(),
                entry.getValue().stream()
                    .sorted(Comparator.comparingInt(m -> m.matchNumber() == null ? 0 : m.matchNumber()))
                    .map(m -> toBracketMatch(m, teamsById))
                    .toList()))
            .toList();

        return new BracketResponse(slug, rounds);
    }

    private BracketMatch toBracketMatch(Match m, Map<String, Team> teamsById) {
        boolean played = m.homeGoals() != null && m.awayGoals() != null;
        String winner = null;
        if (played && m.homeTeamId() != null && m.awayTeamId() != null) {
            try {
                winner = winnerOf(m);
            } catch (RuntimeException ignored) {
                winner = null;
            }
        }
        return new BracketMatch(
            m.id(), m.round(), m.matchNumber(),
            bracketTeam(m.homeTeamId(), teamsById),
            bracketTeam(m.awayTeamId(), teamsById),
            m.homeGoals(), m.awayGoals(), m.homePenalties(), m.awayPenalties(),
            winner, played);
    }

    private BracketTeam bracketTeam(String teamId, Map<String, Team> teamsById) {
        if (teamId == null) {
            return null;
        }
        Team team = teamsById.get(teamId);
        return new BracketTeam(teamId, team != null ? team.name() : null, team != null ? team.code() : null);
    }

    private Map<String, Team> loadTeams(List<Match> knockout) {
        Set<String> ids = new HashSet<>();
        for (Match m : knockout) {
            if (m.homeTeamId() != null) {
                ids.add(m.homeTeamId());
            }
            if (m.awayTeamId() != null) {
                ids.add(m.awayTeamId());
            }
        }
        Map<String, Team> map = new HashMap<>();
        for (Team team : teams.findAllById(ids)) {
            map.put(team.id(), team);
        }
        return map;
    }

    private int[] seedOrder(int n) {
        List<Integer> seeds = new ArrayList<>(List.of(1, 2));
        while (seeds.size() < n) {
            int length = seeds.size() * 2;
            List<Integer> next = new ArrayList<>();
            for (int seed : seeds) {
                next.add(seed);
                next.add(length + 1 - seed);
            }
            seeds = next;
        }
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = seeds.get(i);
        }
        return arr;
    }

    private String roundLabel(int teamsInRound) {
        return switch (teamsInRound) {
            case 2 -> FINAL;
            case 4 -> SEMI_FINAL;
            case 8 -> "QUARTER_FINAL";
            case 16 -> "ROUND_OF_16";
            case 32 -> "ROUND_OF_32";
            default -> "ROUND_OF_" + teamsInRound;
        };
    }

    private int roundSize(String round) {
        return switch (round) {
            case FINAL -> 2;
            case SEMI_FINAL -> 4;
            case "QUARTER_FINAL" -> 8;
            case "ROUND_OF_16" -> 16;
            case "ROUND_OF_32" -> 32;
            default -> Integer.parseInt(round.substring("ROUND_OF_".length()));
        };
    }

    private int displayRank(String round) {
        if (THIRD_PLACE.equals(round)) {
            return 3;
        }
        return roundSize(round);
    }
}
