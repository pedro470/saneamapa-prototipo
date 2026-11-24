package br.edu.cesmac.saneamapa.service;

/**
 * Eventos publicados após mutações de domínio para atualizar o grafo de conhecimento
 * em memória de forma incremental, sem reconstruí-lo do zero a cada leitura.
 */
public sealed interface EventoMutacaoGrafo {
    record RelatoRegistrado(Long relatoId, Long pontoId, boolean novoPonto) implements EventoMutacaoGrafo {}
    record PontoAtribuicaoAlterada(Long pontoId) implements EventoMutacaoGrafo {}
    record VistoriaRegistrada(Long vistoriaId, Long pontoId) implements EventoMutacaoGrafo {}
    record EvidenciaAtualizada(Long evidenciaId) implements EventoMutacaoGrafo {}
    record CamadasAlteradas(Long camadaId) implements EventoMutacaoGrafo {}
    record CensoAlterado() implements EventoMutacaoGrafo {}
    record UsuarioAtualizado(Long usuarioId) implements EventoMutacaoGrafo {}
}
