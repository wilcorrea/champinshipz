package com.championshipz.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.championshipz.config.OpenApiConfig;
import com.championshipz.domain.Championship;
import com.championshipz.domain.Participant;
import com.championshipz.dto.AddParticipantRequest;
import com.championshipz.dto.CreateChampionshipRequest;
import com.championshipz.dto.UpdateChampionshipRequest;
import com.championshipz.service.ChampionshipService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/championships")
@Tag(name = "Championships", description = "Championships and the teams taking part in them")
public class ChampionshipController {

    private final ChampionshipService service;

    public ChampionshipController(ChampionshipService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Create a championship",
        description = """
            The caller becomes the owner and is the only one allowed to change it afterwards.
            Cups require `knockoutSlots` to be a power of two (2, 4, 8, 16, 32).""")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public Championship create(@Valid @RequestBody CreateChampionshipRequest req,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.create(req, jwt.getSubject());
    }

    @GetMapping
    @Operation(summary = "List every championship", description = "Public — no token needed.")
    public List<Championship> list() {
        return service.list();
    }

    @PutMapping("/{slug}")
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Rename a championship or change its slug",
        description = """
            Only the owner can rename. The new slug must be unique — the request is rejected \
            otherwise, and a unique index on the collection is the final guarantee. \
            Changing the slug also repoints every match of the championship, since matches \
            reference it by slug.""")
    @ApiResponse(responseCode = "200", description = "Championship updated")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    @ApiResponse(responseCode = "409", description = "Slug already taken", content = @Content)
    public Championship update(@PathVariable String slug,
                               @Valid @RequestBody UpdateChampionshipRequest req,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.update(slug, req, jwt.getSubject());
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Delete a championship and all of its matches",
        description = """
            Only the owner can delete. Seeded demo championships have no owner and \
            are therefore permanently read-only.""")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public void delete(@PathVariable String slug, @AuthenticationPrincipal Jwt jwt) {
        service.delete(slug, jwt.getSubject());
    }

    @PostMapping("/{slug}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = OpenApiConfig.BEARER)
    @Operation(
        summary = "Enter a team into the championship",
        description = "Cups require a `group`; leagues ignore it.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token", content = @Content)
    public Championship addParticipant(@PathVariable String slug,
                                       @Valid @RequestBody AddParticipantRequest req,
                                       @AuthenticationPrincipal Jwt jwt) {
        return service.addParticipant(slug, req, jwt.getSubject());
    }

    @GetMapping("/{slug}/teams")
    @Operation(summary = "List the teams entered in a championship", description = "Public — no token needed.")
    public List<Participant> participants(@PathVariable String slug) {
        return service.participants(slug);
    }
}
