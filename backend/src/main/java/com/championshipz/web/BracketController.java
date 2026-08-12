package com.championshipz.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.championshipz.dto.BracketResponse;
import com.championshipz.service.BracketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/championships/{slug}/bracket")
@Tag(name = "Bracket", description = "The knockout rounds of a cup")
public class BracketController {

    private final BracketService service;

    public BracketController(BracketService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(
        summary = "Read the knockout bracket of a cup",
        description = """
            Public — no token needed. `rounds` is empty until the group stage finishes; \
            at that point the bracket is generated automatically from the qualified teams. \
            Answers with empty rounds for a league.""")
    public BracketResponse bracket(@PathVariable String slug) {
        return service.getBracket(slug);
    }
}
