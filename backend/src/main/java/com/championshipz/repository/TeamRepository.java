package com.championshipz.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.championshipz.domain.Team;

public interface TeamRepository extends MongoRepository<Team, String> {

    Optional<Team> findByCode(String code);

    boolean existsByCode(String code);
}
