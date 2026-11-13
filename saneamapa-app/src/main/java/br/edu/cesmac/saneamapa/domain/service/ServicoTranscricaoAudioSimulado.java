package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;
import org.springframework.stereotype.Component;

@Component
public class ServicoTranscricaoAudioSimulado implements ServicoTranscricaoAudio {

    @Override
    public String transcrever(String caminhoAudio, TipoOcorrencia contextoOcorrencia, String logradouro) {
        String local = (logradouro != null && !logradouro.isBlank()) ? logradouro : "nossa rua";
        return String.format("Relato em áudio sobre %s na %s. Situação relatada pelo morador para vistoria.",
                contextoOcorrencia.getRotulo().toLowerCase(), local);
    }
}
