package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.model.Relato;
import br.edu.cesmac.saneamapa.repository.RelatoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Prazo de guarda dos dados do morador (RNF20; TBD-29) e dados do controlador exibidos no termo (RNF19; TBD-30).
 * <p>
 * Nome e telefone são eliminados {@code prazo-guarda-dias} depois da primeira vistoria do ponto feita após o relato.
 * Enquanto o ponto aguarda vistoria, os dados ficam guardados, porque o fiscal pode precisar do contato.
 */
@Service
public class PrazoGuardaService {

    private final RelatoRepository relatoRepository;
    private final AuditoriaService auditoriaService;
    private final int prazoGuardaDias;
    private final String controladorNome;
    private final String controladorContato;
    private final String encarregadoNome;
    private final String encarregadoContato;

    public PrazoGuardaService(RelatoRepository relatoRepository,
                              AuditoriaService auditoriaService,
                              @Value("${saneamapa.lgpd.prazo-guarda-dias:180}") int prazoGuardaDias,
                              @Value("${saneamapa.lgpd.controlador-nome:}") String controladorNome,
                              @Value("${saneamapa.lgpd.controlador-contato:}") String controladorContato,
                              @Value("${saneamapa.lgpd.encarregado-nome:}") String encarregadoNome,
                              @Value("${saneamapa.lgpd.encarregado-contato:}") String encarregadoContato) {
        this.relatoRepository = relatoRepository;
        this.auditoriaService = auditoriaService;
        this.prazoGuardaDias = prazoGuardaDias;
        this.controladorNome = controladorNome;
        this.controladorContato = controladorContato;
        this.encarregadoNome = encarregadoNome;
        this.encarregadoContato = encarregadoContato;
    }

    /** Roda todo dia às 3h, depois do recálculo da fila. */
    @Scheduled(cron = "0 0 3 * * *")
    public void eliminarDiariamente() {
        eliminarDadosVencidos(OffsetDateTime.now());
    }

    /**
     * Elimina nome e telefone dos relatos cujo prazo venceu em {@code agora} e registra cada eliminação no log (RF019).
     *
     * @return quantidade de relatos anonimizados
     */
    @Transactional
    public int eliminarDadosVencidos(OffsetDateTime agora) {
        List<Relato> vencidos = relatoRepository.listarComPrazoDeGuardaVencido(agora.minusDays(prazoGuardaDias));
        for (Relato relato : vencidos) {
            relato.anonimizar(agora);
            relatoRepository.save(relato);
            auditoriaService.registrarSistema(TipoEventoAuditoria.ELIMINACAO_DADOS_MORADOR,
                    TipoObjetoAuditoria.RELATO, relato.getId(),
                    "Nome e telefone do relato " + relato.getProtocolo() + " eliminados após " + prazoGuardaDias
                            + " dias da vistoria do ponto (RNF20).");
        }
        return vencidos.size();
    }

    public int getPrazoGuardaDias() {
        return prazoGuardaDias;
    }

    /** Dados que o termo de uso mostra ao morador (RNF19). Campo vazio vira o aviso de configuração pendente. */
    public Map<String, Object> dadosDoTermo() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("controladorNome", textoOuPendente(controladorNome));
        dados.put("controladorContato", textoOuPendente(controladorContato));
        dados.put("encarregadoNome", textoOuPendente(encarregadoNome));
        dados.put("encarregadoContato", textoOuPendente(encarregadoContato));
        dados.put("prazoGuardaDias", prazoGuardaDias);
        return dados;
    }

    private static String textoOuPendente(String valor) {
        return valor == null || valor.isBlank() ? "não configurado nesta instalação" : valor.trim();
    }
}
