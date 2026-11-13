package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import br.edu.cesmac.saneamapa.domain.model.Ponto;

import java.util.List;

public interface ServicoClassificacaoUrgencia {

    void classificarPonto(Ponto ponto, List<CamadaVulnerabilidade> camadasAtivas);
}
