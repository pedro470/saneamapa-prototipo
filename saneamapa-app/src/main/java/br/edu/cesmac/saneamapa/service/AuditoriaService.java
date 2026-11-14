package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.model.LogAuditoria;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.dto.AdminDtos.LogAuditoriaDto;
import br.edu.cesmac.saneamapa.repository.LogAuditoriaRepository;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AuditoriaService {

    private static final ZoneOffset FUSO = ZoneOffset.ofHours(-3);
    private static final OffsetDateTime INICIO_SEM_FILTRO = OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, FUSO);
    private static final OffsetDateTime FIM_SEM_FILTRO = OffsetDateTime.of(9999, 12, 31, 23, 59, 59, 0, FUSO);

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

    @Transactional(readOnly = true)
    public List<LogAuditoriaDto> pesquisar(Long usuarioId, boolean apenasSistema, TipoEventoAuditoria tipoEvento,
                                           LocalDate de, LocalDate ate) {
        // Sem filtro de data, usa limites abertos em vez de null: o PostgreSQL não consegue
        // inferir o tipo de um parâmetro nulo em "(:de IS NULL OR ...)" e a consulta falha.
        OffsetDateTime inicio = de != null ? de.atStartOfDay().atOffset(FUSO) : INICIO_SEM_FILTRO;
        OffsetDateTime fim = ate != null ? ate.plusDays(1).atStartOfDay().minusNanos(1).atOffset(FUSO) : FIM_SEM_FILTRO;
        return logRepository.buscarFiltrado(usuarioId, apenasSistema, tipoEvento, inicio, fim)
                .stream()
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
    }
}
