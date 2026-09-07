package com.pifa.pifabackend.web;

import com.mongodb.client.result.UpdateResult;
import com.pifa.pifabackend.data.GrupoResultado;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}")
    public UpdateResult updateResultado(@PathVariable String id, @RequestBody GrupoResultado resultado) {
        Query query = new Query(Criteria.where("id").is(Integer.parseInt(id))
                .and("grupos.letra").is(resultado.letra()));

        UpdateDefinition update = new Update().set("grupos.$.times", resultado.times());

        return queryBuilder.updateFirst(query, update, "resultados");
    }
}
