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
        OffsetDateTime inicio = de != null ? de.atStartOfDay().atOffset(ZoneOffset.ofHours(-3)) : null;
        OffsetDateTime fim = ate != null ? ate.plusDays(1).atStartOfDay().minusNanos(1).atOffset(ZoneOffset.ofHours(-3)) : null;
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
