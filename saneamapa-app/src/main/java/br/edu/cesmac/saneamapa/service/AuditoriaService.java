package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.model.LogAuditoria;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.dto.AdminDtos.LogAuditoriaDto;
import br.edu.cesmac.saneamapa.dto.AdminDtos.PaginaAuditoriaDto;
import br.edu.cesmac.saneamapa.repository.LogAuditoriaRepository;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AuditoriaService {

    private static final ZoneOffset FUSO = ZoneOffset.ofHours(-3);
    private static final int TAMANHO_PAGINA = 50;
    private static final int DIAS_PADRAO = 30;

    private final LogAuditoriaRepository logRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(LogAuditoriaRepository logRepository, UsuarioRepository usuarioRepository) {
        this.logRepository = logRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public void registrar(Long usuarioId, TipoEventoAuditoria evento, TipoObjetoAuditoria objeto, Long objetoId, String detalhes) {
        Usuario usuario = usuarioId != null ? usuarioRepository.findById(usuarioId).orElse(null) : null;
        logRepository.save(new LogAuditoria(usuario, evento, objeto, objetoId, detalhes));
    }

    @Transactional
    public void registrarSistema(TipoEventoAuditoria evento, TipoObjetoAuditoria objeto, Long objetoId, String detalhes) {
        logRepository.save(new LogAuditoria(null, evento, objeto, objetoId, detalhes));
    }

    /**
     * Consulta paginada (o log só cresce). Sem período informado, mostra os últimos 30 dias.
     */
    @Transactional(readOnly = true)
    public PaginaAuditoriaDto pesquisar(Long usuarioId, boolean apenasSistema, TipoEventoAuditoria tipoEvento,
                                        LocalDate de, LocalDate ate, int pagina) {
        LocalDate hoje = LocalDate.now(FUSO);
        LocalDate fimPeriodo = ate != null ? ate : hoje;
        LocalDate inicioPeriodo = de != null ? de : fimPeriodo.minusDays(DIAS_PADRAO);
        OffsetDateTime inicio = inicioPeriodo.atStartOfDay().atOffset(FUSO);
        OffsetDateTime fim = fimPeriodo.plusDays(1).atStartOfDay().minusNanos(1).atOffset(FUSO);

        Page<LogAuditoria> resultado = logRepository.buscarFiltrado(usuarioId, apenasSistema, tipoEvento, inicio, fim,
                PageRequest.of(Math.max(0, pagina), TAMANHO_PAGINA));
        List<LogAuditoriaDto> itens = resultado.getContent().stream()
                .map(l -> new LogAuditoriaDto(
                        l.getId(),
                        l.getDataHora(),
                        l.getAtorNome(),
                        l.getTipoEvento(),
                        l.getTipoEvento().getRotulo(),
                        l.getTipoObjeto().name(),
                        l.getObjetoId(),
                        l.getDetalhes()
                ))
                .toList();
        return new PaginaAuditoriaDto(itens, resultado.getNumber(), resultado.getTotalPages(),
                resultado.getTotalElements(), inicioPeriodo, fimPeriodo);
    }
}
