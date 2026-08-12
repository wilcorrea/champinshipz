package com.championshipz.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.championshipz.domain.Championship;

public interface ChampionshipRepository extends MongoRepository<Championship, String> {

    Optional<Championship> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
