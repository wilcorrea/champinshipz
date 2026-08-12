package com.championshipz.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.championshipz.domain.Team;
import com.championshipz.dto.CreateTeamRequest;
import com.championshipz.repository.TeamRepository;
import com.championshipz.web.NotFoundException;

@Service
public class TeamService {

    private final TeamRepository teams;

    public TeamService(TeamRepository teams) {
        this.teams = teams;
    }

    public Team create(CreateTeamRequest req) {
        if (teams.existsByCode(req.code())) {
            throw new IllegalArgumentException("A team with code '" + req.code() + "' already exists");
        }
        return teams.save(new Team(null, req.name(), req.code()));
    }

    public List<Team> list() {
        return teams.findAll();
    }

    public Team getById(String id) {
        return teams.findById(id)
            .orElseThrow(() -> new NotFoundException("Team '" + id + "' not found"));
    }
}
