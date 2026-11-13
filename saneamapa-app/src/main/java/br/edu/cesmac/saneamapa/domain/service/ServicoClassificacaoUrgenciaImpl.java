package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.model.CamadaVulnerabilidade;
import br.edu.cesmac.saneamapa.domain.model.Ponto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServicoClassificacaoUrgenciaImpl implements ServicoClassificacaoUrgencia {

    @Override
    public void classificarPonto(Ponto ponto, List<CamadaVulnerabilidade> camadasAtivas) {
        int maiorPeso = 0;
        CamadaVulnerabilidade camadaMotivadora = null;

        double lat = ponto.getLatitude().doubleValue();
        double lon = ponto.getLongitude().doubleValue();

        for (CamadaVulnerabilidade camada : camadasAtivas) {
            if (!camada.isAtiva()) {
                continue;
            }
            if (camada.obterGeometria().contemPonto(lat, lon) && camada.getPesoUrgencia() > maiorPeso) {
                maiorPeso = camada.getPesoUrgencia();
                camadaMotivadora = camada;
            }
        }

        String motivo;
        if (camadaMotivadora == null) {
            motivo = "Normal: nenhuma camada de vulnerabilidade no local.";
        } else {
            String nivelRotulo = maiorPeso == 3 ? "Crítica" : "Elevada";
            motivo = String.format("%s pela camada %s (%s).",
                    nivelRotulo, camadaMotivadora.getNome(), camadaMotivadora.getCategoria().getRotulo());
        }
        ponto.aplicarClassificacaoUrgencia(maiorPeso, motivo);
    }
}
