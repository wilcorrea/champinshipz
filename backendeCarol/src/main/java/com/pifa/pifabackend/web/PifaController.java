package com.pifa.pifabackend.web;

import com.pifa.pifabackend.domain.Campeonato;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping

public class PifaController {

    public List<Campeonato> getCampeonatos(){
        List<Campeonato> campeonatos = new ArrayList<>();
        campeonatos.add(new Campeonato());
        return campeonatos;
    }
}
