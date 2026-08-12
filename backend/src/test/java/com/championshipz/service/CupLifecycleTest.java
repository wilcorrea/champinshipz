package com.championshipz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.championshipz.domain.Championship;
import com.championshipz.domain.ChampionshipType;
import com.championshipz.domain.Team;
import com.championshipz.dto.AddParticipantRequest;
import com.championshipz.dto.BracketResponse;
import com.championshipz.dto.BracketRound;
import com.championshipz.dto.CreateChampionshipRequest;
import com.championshipz.dto.CreateMatchRequest;
import com.championshipz.dto.MatchResultRequest;
import com.championshipz.dto.StandingsResponse;
import com.championshipz.repository.ChampionshipRepository;
import com.championshipz.repository.MatchRepository;
import com.championshipz.repository.TeamRepository;

/**
 * O ciclo inteiro de uma copa, do zero ao campeão — o mesmo caminho que o app percorre.
 * Vale como rede contra regressão no chaveamento, que é a parte com mais regra escondida.
 */
@SpringBootTest
class CupLifecycleTest {

    private static final String OWNER = "user_lifecycle";
    private static final String SLUG = "lifecycle-cup";

    @Autowired ChampionshipService championshipsService;
    @Autowired MatchService matchesService;
    @Autowired StandingsService standingsService;
    @Autowired BracketService bracketService;

    @Autowired ChampionshipRepository championships;
    @Autowired MatchRepository matches;
    @Autowired TeamRepository teams;

    private final List<String> created = new ArrayList<>();

    @BeforeEach
    void clean() {
        championships.findBySlug(SLUG).ifPresent(championships::delete);
        matches.deleteByChampionship(SLUG);
        created.clear();
    }

    private String team(String name, String code) {
        Team saved = teams.save(new Team(null, name, code));
        created.add(saved.id());
        return saved.id();
    }

    private String nameOf(String teamId) {
        return teams.findById(teamId).map(Team::name).orElseThrow();
    }

    private BracketRound round(BracketResponse bracket, String name) {
        return bracket.rounds().stream()
            .filter(candidate -> candidate.round().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Round " + name + " ausente"));
    }

    @Test
    void aCupRunsFromAnEmptyBracketToAChampion() {
        // 1. Copa com 4 vagas no mata-mata.
        Championship cup = championshipsService.create(new CreateChampionshipRequest(
            "Copa do Teste", SLUG, ChampionshipType.CUP, 4, null), OWNER);
        assertThat(cup.ownerId()).isEqualTo(OWNER);

        // 2. Oito times, dois grupos de quatro.
        List<String> groupA = List.of(
            team("Alfa", "ALF"), team("Bravo", "BRA"),
            team("Charlie", "CHA"), team("Delta", "DEL"));
        List<String> groupB = List.of(
            team("Echo", "ECH"), team("Foxtrot", "FOX"),
            team("Golf", "GOL"), team("Hotel", "HOT"));

        groupA.forEach(id -> championshipsService.addParticipant(
            SLUG, new AddParticipantRequest(id, "A"), OWNER));
        groupB.forEach(id -> championshipsService.addParticipant(
            SLUG, new AddParticipantRequest(id, "B"), OWNER));

        // Antes dos jogos o chaveamento não existe.
        assertThat(bracketService.getBracket(SLUG).rounds()).isEmpty();

        // 3. Os doze jogos de grupo. O placar é montado para a ordem de classificação
        //    ficar determinística: o primeiro time de cada lista vence todos, e assim por diante.
        playGroup(groupA);
        playGroup(groupB);

        // 4. O chaveamento se materializa sozinho ao entrar o último jogo de grupo.
        BracketResponse bracket = bracketService.getBracket(SLUG);
        assertThat(bracket.rounds()).extracting(BracketRound::round)
            .containsExactlyInAnyOrder("SEMI_FINAL", "THIRD_PLACE", "FINAL");

        // 5. Classificaram os dois primeiros de cada grupo, cruzados 1ºA x 2ºB e 1ºB x 2ºA.
        StandingsResponse standings = standingsService.compute(SLUG);
        assertThat(standings.groups()).hasSize(2);
        assertThat(standings.groups().stream()
            .flatMap(group -> group.rows().stream())
            .filter(row -> "QUALIFIED".equals(row.status()))
            .count()).isEqualTo(4);

        BracketRound semis = round(bracket, "SEMI_FINAL");
        assertThat(semis.matches()).hasSize(2);
        assertThat(semis.matches()).allSatisfy(match -> {
            assertThat(match.home()).isNotNull();
            assertThat(match.away()).isNotNull();
            assertThat(match.played()).isFalse();
        });

        String semiOneId = semis.matches().get(0).id();
        String semiTwoId = semis.matches().get(1).id();
        String semiOneHome = semis.matches().get(0).home().teamId();
        String semiTwoHome = semis.matches().get(1).home().teamId();
        String semiOneAway = semis.matches().get(0).away().teamId();
        String semiTwoAway = semis.matches().get(1).away().teamId();

        // 6. Uma semi decidida no tempo normal; a outra, nos pênaltis.
        matchesService.setResult(SLUG, semiOneId, new MatchResultRequest(2, 1, null, null), OWNER);
        matchesService.setResult(SLUG, semiTwoId, new MatchResultRequest(1, 1, 4, 2), OWNER);

        // Empate sem pênaltis é recusado.
        assertThatThrownBy(() -> matchesService.setResult(
            SLUG, semiOneId, new MatchResultRequest(0, 0, null, null), OWNER))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("penalties");

        // 7. Vencedores foram para a final e perdedores para a disputa de 3º.
        bracket = bracketService.getBracket(SLUG);
        BracketRound finalRound = round(bracket, "FINAL");
        BracketRound thirdRound = round(bracket, "THIRD_PLACE");

        assertThat(finalRound.matches().get(0).home().teamId()).isEqualTo(semiOneHome);
        assertThat(finalRound.matches().get(0).away().teamId()).isEqualTo(semiTwoHome);
        assertThat(thirdRound.matches().get(0).home().teamId()).isEqualTo(semiOneAway);
        assertThat(thirdRound.matches().get(0).away().teamId()).isEqualTo(semiTwoAway);

        // 8. Disputa de 3º e final.
        matchesService.setResult(SLUG, thirdRound.matches().get(0).id(),
            new MatchResultRequest(3, 2, null, null), OWNER);
        matchesService.setResult(SLUG, finalRound.matches().get(0).id(),
            new MatchResultRequest(0, 1, null, null), OWNER);

        // 9. Campeão é o visitante da final — o vencedor da semi dos pênaltis.
        bracket = bracketService.getBracket(SLUG);
        var decided = round(bracket, "FINAL").matches().get(0);
        assertThat(decided.played()).isTrue();
        assertThat(decided.winnerTeamId()).isEqualTo(semiTwoHome);

        assertThat(round(bracket, "THIRD_PLACE").matches().get(0).winnerTeamId())
            .isEqualTo(semiOneAway);

        System.out.println("Campeão: " + nameOf(decided.winnerTeamId()));
    }

    /** Todos contra todos dentro do grupo, com o time mais à esquerda sempre levando a melhor. */
    private void playGroup(List<String> group) {
        for (int i = 0; i < group.size(); i++) {
            for (int j = i + 1; j < group.size(); j++) {
                matchesService.register(SLUG,
                    new CreateMatchRequest(group.get(i), group.get(j), 2, 0), OWNER);
            }
        }
    }
}
