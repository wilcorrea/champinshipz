package com.championshipz.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.championshipz.config.OpenApiConfig;
import com.championshipz.domain.Team;
import com.championshipz.dto.CreateTeamRequest;
import com.championshipz.service.TeamService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teams")
@Tag(name = "Teams", description = "The shared catalogue championships draw their teams from")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Add a team to the catalogue",
        description = """
            Teams have no owner: the catalogue is shared and one team can take part in any \
            number of championships. Using an ISO country code (BRA, ARG, POR) lets clients \
            render a flag without storing an image.""")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public Team create(@Valid @RequestBody CreateTeamRequest req) {
        return service.create(req);
    }

    @GetMapping
    @Operation(summary = "List the team catalogue", description = "Public — no token needed.")
    public List<Team> list() {
        return service.list();
    }
}
