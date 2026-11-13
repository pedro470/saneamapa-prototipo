package br.edu.cesmac.saneamapa.domain.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoOcorrencia;

/**
 * Abstração do serviço de transcrição de áudio por IA (RF003 nota 5 / RS007).
 * Recebe apenas metadados do áudio, sem nome nem telefone do morador.
 */
public interface ServicoTranscricaoAudio {

    String transcrever(String caminhoAudio, TipoOcorrencia contextoOcorrencia, String logradouro);
}
