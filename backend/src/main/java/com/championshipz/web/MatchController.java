package com.championshipz.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.championshipz.config.OpenApiConfig;
import com.championshipz.domain.Match;
import com.championshipz.dto.CreateMatchRequest;
import com.championshipz.dto.MatchResultRequest;
import com.championshipz.service.MatchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/championships/{slug}/matches")
@Tag(name = "Matches", description = "Recording results — every standing is derived from these")
public class MatchController {

    private final MatchService service;

    public MatchController(MatchService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Record a league or group-stage match",
        description = """
            Both teams must already be entered in the championship. In a cup, once the last \
            group-stage match lands, the knockout bracket is generated automatically.""")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public Match register(@PathVariable String slug,
                          @Valid @RequestBody CreateMatchRequest req,
                          @AuthenticationPrincipal Jwt jwt) {
        return service.register(slug, req, jwt.getSubject());
    }

    @PostMapping("/{matchId}/result")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Set the result of a knockout match",
        description = """
            A draw must be settled on penalties — send `homePenalties` and `awayPenalties`, \
            and those cannot be equal either. The winner is placed into the next round \
            automatically, and a losing semi-finalist drops into the third-place match.""")
    @ApiResponse(responseCode = "200", description = "Result recorded")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public Match setResult(@PathVariable String slug, @PathVariable String matchId,
                           @Valid @RequestBody MatchResultRequest req,
                           @AuthenticationPrincipal Jwt jwt) {
        return service.setResult(slug, matchId, req, jwt.getSubject());
    }
}
