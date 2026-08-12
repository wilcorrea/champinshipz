package com.championshipz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.championshipz.domain.Match;
import com.championshipz.domain.MatchStage;

public interface MatchRepository extends MongoRepository<Match, String> {

    List<Match> findByChampionshipOrderByPlayedAtAsc(String championship);

    List<Match> findByChampionshipAndStage(String championship, MatchStage stage);

    boolean existsByChampionshipAndStage(String championship, MatchStage stage);

    Optional<Match> findByChampionshipAndRoundAndMatchNumber(String championship, String round, Integer matchNumber);

    void deleteByChampionship(String championship);
}
