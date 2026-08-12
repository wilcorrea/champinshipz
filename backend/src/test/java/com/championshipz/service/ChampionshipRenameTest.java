package com.championshipz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Match;
import com.championshipz.domain.MatchStage;
import com.championshipz.domain.Participant;
import com.championshipz.dto.UpdateChampionshipRequest;
import com.championshipz.repository.ChampionshipRepository;
import com.championshipz.repository.MatchRepository;
import com.championshipz.web.ForbiddenException;

@SpringBootTest
class ChampionshipRenameTest {

    private static final String OWNER = "user_test";
    private static final String OLD_SLUG = "rename-test-old";
    private static final String NEW_SLUG = "rename-test-new";
    private static final String OTHER_SLUG = "rename-test-taken";

    @Autowired ChampionshipService service;
    @Autowired ChampionshipRepository championships;
    @Autowired MatchRepository matches;

    @BeforeEach
    void clean() {
        for (String slug : List.of(OLD_SLUG, NEW_SLUG, OTHER_SLUG)) {
            championships.findBySlug(slug).ifPresent(championships::delete);
            matches.deleteByChampionship(slug);
        }
    }

    private Championship seed(String slug, String ownerId) {
        return championships.save(new Championship(
            null, "Antes", slug, ChampionshipType.LEAGUE,
            List.of(new Participant("team-a", null), new Participant("team-b", null)),
            null, ChampionshipService.DEFAULT_STANDINGS_COLUMNS, ownerId));
    }

    private void seedMatch(String slug) {
        matches.save(new Match(
            null, slug, MatchStage.GROUP, null, null, null,
            "team-a", "team-b", 3, 1, null, null, Instant.now()));
    }

    @Test
    void renamingKeepsTheMatchesAttached() {
        seed(OLD_SLUG, OWNER);
        seedMatch(OLD_SLUG);

        Championship updated = service.update(
            OLD_SLUG, new UpdateChampionshipRequest("Depois", NEW_SLUG), OWNER);

        assertThat(updated.name()).isEqualTo("Depois");
        assertThat(updated.slug()).isEqualTo(NEW_SLUG);

        assertThat(matches.findByChampionshipOrderByPlayedAtAsc(OLD_SLUG)).isEmpty();
        assertThat(matches.findByChampionshipOrderByPlayedAtAsc(NEW_SLUG))
            .singleElement()
            .satisfies(match -> {
                assertThat(match.homeGoals()).isEqualTo(3);
                assertThat(match.awayGoals()).isEqualTo(1);
            });
    }

    @Test
    void renamingOnlyTheNameLeavesTheSlugAndMatchesAlone() {
        seed(OLD_SLUG, OWNER);
        seedMatch(OLD_SLUG);

        service.update(OLD_SLUG, new UpdateChampionshipRequest("Só o nome", OLD_SLUG), OWNER);

        assertThat(championships.findBySlug(OLD_SLUG))
            .get()
            .satisfies(found -> assertThat(found.name()).isEqualTo("Só o nome"));
        assertThat(matches.findByChampionshipOrderByPlayedAtAsc(OLD_SLUG)).hasSize(1);
    }

    @Test
    void aTakenSlugIsRejected() {
        seed(OLD_SLUG, OWNER);
        seed(OTHER_SLUG, OWNER);

        assertThatThrownBy(() ->
            service.update(OLD_SLUG, new UpdateChampionshipRequest("Depois", OTHER_SLUG), OWNER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(OTHER_SLUG);

        assertThat(championships.findBySlug(OLD_SLUG)).isPresent();
    }

    @Test
    void onlyTheOwnerCanRename() {
        seed(OLD_SLUG, OWNER);

        assertThatThrownBy(() ->
            service.update(OLD_SLUG, new UpdateChampionshipRequest("Depois", NEW_SLUG), "user_other"))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aDemoChampionshipCannotBeRenamed() {
        seed(OLD_SLUG, null);

        assertThatThrownBy(() ->
            service.update(OLD_SLUG, new UpdateChampionshipRequest("Depois", NEW_SLUG), OWNER))
            .isInstanceOf(ForbiddenException.class);
    }
}
