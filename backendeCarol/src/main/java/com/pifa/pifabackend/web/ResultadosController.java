package com.pifa.pifabackend.web;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resultados")
public class ResultadosController {

    private final MongoTemplate queryBuilder;

    public ResultadosController(MongoTemplate mongoTemplate){
        this.queryBuilder = mongoTemplate;
    }

    @GetMapping
    public List<Map<String, Object>> getResultados(){
        return new ArrayList<>(queryBuilder.findAll(Document.class, "resultados"));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getResultado(@PathVariable String id) {
        Query query = new Query(Criteria.where("id").is(Integer.parseInt(id)));
        return queryBuilder.findOne(query, Document.class, "resultados");
    }
}
