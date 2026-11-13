package br.edu.cesmac.saneamapa.service;

import br.edu.cesmac.saneamapa.domain.enums.TipoEventoAuditoria;
import br.edu.cesmac.saneamapa.domain.enums.TipoObjetoAuditoria;
import br.edu.cesmac.saneamapa.domain.exception.ConflitoDadosException;
import br.edu.cesmac.saneamapa.domain.exception.RecursoNaoEncontradoException;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.model.AlteracaoUsuario;
import br.edu.cesmac.saneamapa.domain.model.Usuario;
import br.edu.cesmac.saneamapa.dto.AdminDtos.AlteracaoUsuarioDto;
import br.edu.cesmac.saneamapa.dto.AdminDtos.AtualizarUsuarioRequest;
import br.edu.cesmac.saneamapa.dto.AdminDtos.CriarUsuarioRequest;
import br.edu.cesmac.saneamapa.dto.AdminDtos.UsuarioDto;
import br.edu.cesmac.saneamapa.repository.AlteracaoUsuarioRepository;
import br.edu.cesmac.saneamapa.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AlteracaoUsuarioRepository alteracaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          AlteracaoUsuarioRepository alteracaoRepository,
                          PasswordEncoder passwordEncoder,
                          AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.alteracaoRepository = alteracaoRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDto> listarTodos() {
        return usuarioRepository.findAllByOrderByNomeCompletoAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AlteracaoUsuarioDto> listarAlteracoesRecentes() {
        return alteracaoRepository.findTop30ByOrderByDataHoraDesc().stream()
                .map(a -> new AlteracaoUsuarioDto(
                        a.getDataHora(),
                        a.getAutor().getNomeCompleto(),
                        a.getUsuarioAlvo().getNomeCompleto(),
                        a.getCampoAlterado(),
                        a.getValorAnterior(),
                        a.getValorNovo()
                ))
                .toList();
    }

    @Transactional
    public UsuarioDto criar(CriarUsuarioRequest req, Long autorId) {
        validarPoliticaSenha(req.senha());
        if (usuarioRepository.existsByCpf(req.cpf())) {
            throw new ConflitoDadosException(
                    "CPF já cadastrado.",
                    "Procure a pessoa na lista de usuários: ela pode estar desativada."
            );
        }
        if (usuarioRepository.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflitoDadosException(
                    "E-mail já cadastrado.",
                    "Utilize outro endereço de e-mail institucional."
            );
        }

        Usuario novo = new Usuario(
                req.cpf(),
                req.nomeCompleto(),
                req.email(),
                req.celular(),
                passwordEncoder.encode(req.senha()),
                req.perfil(),
                req.ativo()
        );
        novo = usuarioRepository.save(novo);
        auditoriaService.registrar(autorId, TipoEventoAuditoria.ALTERACAO_USUARIO,
                TipoObjetoAuditoria.USUARIO, novo.getId(), "Cadastro de usuário " + novo.getNomeCompleto());
        return toDto(novo);
    }

    @Transactional
    public UsuarioDto atualizar(Long id, AtualizarUsuarioRequest req, Long autorId) {
        Usuario alvo = buscarOuFalhar(id);
        Usuario autor = buscarOuFalhar(autorId);

        if (!alvo.getEmail().equalsIgnoreCase(req.email().trim())
                && usuarioRepository.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ConflitoDadosException("E-mail já cadastrado.", "Escolha outro e-mail que ainda não esteja em uso.");
        }

        registrarMudanca(alvo, autor, "Nome", alvo.getNomeCompleto(), req.nomeCompleto().trim());
        registrarMudanca(alvo, autor, "E-mail", alvo.getEmail(), req.email().trim().toLowerCase());
        registrarMudanca(alvo, autor, "Celular", alvo.getCelular(), req.celular().trim());
        registrarMudanca(alvo, autor, "Perfil", alvo.getPerfil().getRotulo(), req.perfil().getRotulo());
        registrarMudanca(alvo, autor, "Ativo", alvo.isAtivo() ? "Sim" : "Não", req.ativo() ? "Sim" : "Não");

        alvo.atualizarDadosCadastrais(req.nomeCompleto(), req.email(), req.celular(), req.perfil(), req.ativo());
        usuarioRepository.save(alvo);

        auditoriaService.registrar(autorId, TipoEventoAuditoria.ALTERACAO_USUARIO,
                TipoObjetoAuditoria.USUARIO, alvo.getId(), "Alteração cadastral de " + alvo.getNomeCompleto());
        return toDto(alvo);
    }

    @Transactional
    public UsuarioDto alternarAtivo(Long id, Long autorId) {
        Usuario alvo = buscarOuFalhar(id);
        Usuario autor = buscarOuFalhar(autorId);

        boolean novoStatus = !alvo.isAtivo();
        registrarMudanca(alvo, autor, "Ativo", alvo.isAtivo() ? "Sim" : "Não", novoStatus ? "Sim" : "Não");
        alvo.alterarAtivo(novoStatus);
        usuarioRepository.save(alvo);

        auditoriaService.registrar(autorId, TipoEventoAuditoria.ALTERACAO_USUARIO,
                TipoObjetoAuditoria.USUARIO, alvo.getId(),
                alvo.getNomeCompleto() + (novoStatus ? " reativado." : " desativado."));
        return toDto(alvo);
    }

    private void registrarMudanca(Usuario alvo, Usuario autor, String campo, String antes, String depois) {
        if (!Objects.equals(antes, depois)) {
            alteracaoRepository.save(new AlteracaoUsuario(alvo, autor, campo, antes, depois));
        }
    }

    private void validarPoliticaSenha(String senha) {
        if (senha == null || senha.length() < 8
                || !senha.chars().anyMatch(Character::isLetter)
                || !senha.chars().anyMatch(Character::isDigit)) {
            throw new RegraNegocioException(
                    "Senha não atende à política de segurança (RNF16).",
                    "A senha deve ter ao menos 8 caracteres, combinando letras e números."
            );
        }
    }

    private Usuario buscarOuFalhar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado.", "Atualize a lista de usuários."));
    }

    private UsuarioDto toDto(Usuario u) {
        return new UsuarioDto(
                u.getId(),
                u.getCpfMascarado(),
                u.getNomeCompleto(),
                u.getEmail(),
                u.getCelular(),
                u.getPerfil(),
                u.isAtivo()
        );
    }
}
