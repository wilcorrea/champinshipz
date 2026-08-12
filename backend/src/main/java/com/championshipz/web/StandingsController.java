package com.championshipz.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.championshipz.dto.StandingsResponse;
import com.championshipz.service.StandingsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/championships/{slug}/standings")
@Tag(name = "Standings", description = "The table, computed on the fly from the matches")
public class StandingsController {

    private final StandingsService service;

    public StandingsController(StandingsService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
        summary = "Compute the standings of a championship",
        description = """
            Public — no token needed. Nothing here is stored: points, goal difference and \
            form are derived from the recorded matches on every request. A league answers \
            with a single `table`; a cup answers with one entry per `group`, and \
            `standingsColumns` tells the client which columns to show.""")
    public StandingsResponse standings(@PathVariable String slug) {
        return service.compute(slug);
    }
}
