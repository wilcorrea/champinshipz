package com.championshipz.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Match;
import com.championshipz.domain.MatchStage;
import com.championshipz.domain.Participant;
import com.championshipz.domain.Team;
import com.championshipz.dto.GroupStanding;
import com.championshipz.dto.StandingRow;
import com.championshipz.dto.StandingsResponse;
import com.championshipz.repository.MatchRepository;
import com.championshipz.repository.TeamRepository;

@Service
public class StandingsService {

    private final ChampionshipService championships;
    private final TeamRepository teams;
    private final MatchRepository matches;

    public StandingsService(ChampionshipService championships, TeamRepository teams, MatchRepository matches) {
        this.championships = championships;
        this.teams = teams;
        this.matches = matches;
    }

    public StandingsResponse compute(String slug) {
        Championship championship = championships.getBySlug(slug);
        Map<String, Team> teamsById = loadTeams(championship);

        List<Match> played = matches.findByChampionshipOrderByPlayedAtAsc(slug).stream()
            .filter(m -> m.homeGoals() != null && m.awayGoals() != null)
            .toList();

        if (championship.type() == ChampionshipType.LEAGUE) {
            List<TeamStats> stats = standingsFor(championship.teams(), played, MatchStage.LEAGUE, null);
            return new StandingsResponse(
                championship.type(), championship.standingsColumns(), null, toRows(stats, teamsById));
        }

        Map<String, List<Participant>> byGroup = championship.teams().stream()
            .collect(Collectors.groupingBy(
                p -> p.group() == null ? "" : p.group(), TreeMap::new, Collectors.toList()));

        List<String> groupNames = new ArrayList<>();
        List<List<TeamStats>> groupStats = new ArrayList<>();
        for (Map.Entry<String, List<Participant>> entry : byGroup.entrySet()) {
            groupNames.add(entry.getKey());
            groupStats.add(standingsFor(entry.getValue(), played, MatchStage.GROUP, entry.getKey()));
        }
        applyQualification(championship, groupStats);

        List<GroupStanding> groups = new ArrayList<>();
        for (int i = 0; i < groupNames.size(); i++) {
            groups.add(new GroupStanding(groupNames.get(i), toRows(groupStats.get(i), teamsById)));
        }
        return new StandingsResponse(championship.type(), championship.standingsColumns(), groups, null);
    }

    private List<TeamStats> standingsFor(List<Participant> participants, List<Match> played,
                                         MatchStage stage, String group) {
        Map<String, TeamStats> statsById = new LinkedHashMap<>();
        for (Participant p : participants) {
            statsById.put(p.teamId(), new TeamStats(p.teamId()));
        }

        for (Match m : played) {
            if (m.stage() != stage) {
                continue;
            }
            if (stage == MatchStage.GROUP && !Objects.equals(m.group(), group)) {
                continue;
            }
            TeamStats home = statsById.get(m.homeTeamId());
            TeamStats away = statsById.get(m.awayTeamId());
            if (home == null || away == null) {
                continue;
            }

            int homeGoals = m.homeGoals();
            int awayGoals = m.awayGoals();
            home.matchesPlayed++;
            away.matchesPlayed++;
            home.goalsFor += homeGoals;
            home.goalsAgainst += awayGoals;
            away.goalsFor += awayGoals;
            away.goalsAgainst += homeGoals;

            if (homeGoals > awayGoals) {
                home.wins++;
                away.losses++;
                home.form.add("WIN");
                away.form.add("LOSS");
            } else if (homeGoals < awayGoals) {
                away.wins++;
                home.losses++;
                away.form.add("WIN");
                home.form.add("LOSS");
            } else {
                home.draws++;
                away.draws++;
                home.form.add("DRAW");
                away.form.add("DRAW");
            }
        }

        List<TeamStats> ordered = new ArrayList<>(statsById.values());
        ordered.sort(STANDING_ORDER);
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).position = i + 1;
        }
        return ordered;
    }

    private void applyQualification(Championship championship, List<List<TeamStats>> groups) {
        Integer slots = championship.knockoutSlots();
        if (slots == null || groups.isEmpty()) {
            return;
        }
        int numGroups = groups.size();
        int base = slots / numGroups;
        int extra = slots % numGroups;

        for (List<TeamStats> group : groups) {
            for (int i = 0; i < group.size(); i++) {
                group.get(i).status = i < base ? "QUALIFIED" : "ELIMINATED";
            }
        }

        if (extra > 0) {
            List<TeamStats> contenders = new ArrayList<>();
            for (List<TeamStats> group : groups) {
                if (group.size() > base) {
                    contenders.add(group.get(base));
                }
            }
            contenders.sort(STANDING_ORDER);
            for (int i = 0; i < Math.min(extra, contenders.size()); i++) {
                contenders.get(i).status = "QUALIFIED";
            }
        }
    }

    private List<StandingRow> toRows(List<TeamStats> stats, Map<String, Team> teamsById) {
        List<StandingRow> rows = new ArrayList<>();
        for (TeamStats s : stats) {
            Team team = teamsById.get(s.teamId);
            rows.add(new StandingRow(
                s.position, s.status, s.teamId,
                team != null ? team.name() : s.teamId,
                team != null ? team.code() : null,
                s.matchesPlayed, s.wins, s.draws, s.losses,
                s.goalsFor, s.goalsAgainst, s.goalDifference(), s.points(),
                List.copyOf(s.form)));
        }
        return rows;
    }

    private Map<String, Team> loadTeams(Championship championship) {
        List<String> ids = championship.teams().stream().map(Participant::teamId).toList();
        Map<String, Team> map = new HashMap<>();
        for (Team team : teams.findAllById(ids)) {
            map.put(team.id(), team);
        }
        return map;
    }

    private static final Comparator<TeamStats> STANDING_ORDER = Comparator
        .comparingInt(TeamStats::points).reversed()
        .thenComparing(Comparator.comparingInt(TeamStats::goalDifference).reversed())
        .thenComparing(Comparator.comparingInt((TeamStats t) -> t.goalsFor).reversed())
        .thenComparing(t -> t.teamId);

    private static final class TeamStats {
        final String teamId;
        int matchesPlayed;
        int wins;
        int draws;
        int losses;
        int goalsFor;
        int goalsAgainst;
        int position;
        String status;
        final List<String> form = new ArrayList<>();

        TeamStats(String teamId) {
            this.teamId = teamId;
        }

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        int points() {
            return wins * 3 + draws;
        }
    }
}
