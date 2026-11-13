package br.edu.cesmac.saneamapa.domain.model;

import br.edu.cesmac.saneamapa.domain.enums.PerfilUsuario;
import br.edu.cesmac.saneamapa.domain.exception.AcessoNegadoException;
import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import br.edu.cesmac.saneamapa.domain.service.CpfUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/**
 * Usuário da equipe interna (Fiscal ou Prefeitura).
 * Encapsula validação de CPF, política de senha (RNF16), bloqueio após 5 falhas e imutabilidade do CPF (RF013).
 */
@Entity
@Table(name = "usuario")
public class Usuario extends EntidadeAuditavel {

    private static final int MAX_TENTATIVAS_FALHAS = 5;
    private static final int MINUTOS_BLOQUEIO = 15;

    @Column(nullable = false, unique = true, length = 14, updatable = false)
    private String cpf;

    @Column(name = "nome_completo", nullable = false, length = 120)
    private String nomeCompleto;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 15)
    private String celular;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "tentativas_falhas", nullable = false)
    private short tentativasFalhas = 0;

    @Column(name = "bloqueado_ate")
    private OffsetDateTime bloqueadoAte;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    protected Usuario() {
    }

    public Usuario(String cpf, String nomeCompleto, String email, String celular, String senhaHash, PerfilUsuario perfil, boolean ativo) {
        if (!CpfUtils.isCpfValido(cpf)) {
            throw new RegraNegocioException("CPF inválido.", "Informe um CPF válido com 11 dígitos e dígitos verificadores corretos.");
        }
        if (!CpfUtils.isTelefoneValido(celular)) {
            throw new RegraNegocioException("Celular inválido.", "Use o formato (xx) xxxxx-xxxx ou (xx) xxxx-xxxx.");
        }
        if (nomeCompleto == null || nomeCompleto.trim().length() < 3) {
            throw new RegraNegocioException("Nome completo inválido.", "Informe o nome completo com ao menos 3 caracteres.");
        }
        if (email == null || !email.contains("@")) {
            throw new RegraNegocioException("E-mail inválido.", "Informe um endereço de e-mail válido.");
        }
        this.cpf = cpf;
        this.nomeCompleto = nomeCompleto.trim();
        this.email = email.trim().toLowerCase();
        this.celular = celular.trim();
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
        this.atualizadoEm = OffsetDateTime.now();
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = OffsetDateTime.now();
    }

    public boolean estaBloqueado(OffsetDateTime agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }

    public void validarPodeAutenticar(OffsetDateTime agora) {
        if (!ativo) {
            throw new AcessoNegadoException(
                "Usuário desativado não pode acessar o sistema.",
                "Solicite à Prefeitura a reativação da sua conta no cadastro de usuários."
            );
        }
        if (estaBloqueado(agora)) {
            throw new AcessoNegadoException(
                "Conta bloqueada temporariamente por excesso de tentativas incorretas.",
                "Aguarde 15 minutos ou utilize a opção de redefinição de senha."
            );
        }
    }

    public void registrarFalhaAutenticacao(OffsetDateTime agora) {
        if (bloqueadoAte != null && !bloqueadoAte.isAfter(agora)) {
            this.tentativasFalhas = 0;
            this.bloqueadoAte = null;
        }
        this.tentativasFalhas++;
        if (this.tentativasFalhas >= MAX_TENTATIVAS_FALHAS) {
            this.bloqueadoAte = agora.plusMinutes(MINUTOS_BLOQUEIO);
        }
    }

    public void registrarSucessoAutenticacao() {
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public int tentativasRestantes() {
        return Math.max(0, MAX_TENTATIVAS_FALHAS - this.tentativasFalhas);
    }

    public void atualizarDadosCadastrais(String nomeCompleto, String email, String celular, PerfilUsuario perfil, boolean ativo) {
        if (nomeCompleto == null || nomeCompleto.trim().length() < 3) {
            throw new RegraNegocioException("Nome inválido.", "Informe ao menos 3 caracteres para o nome completo.");
        }
        if (email == null || !email.contains("@")) {
            throw new RegraNegocioException("E-mail inválido.", "Informe um endereço de e-mail válido.");
        }
        if (!CpfUtils.isTelefoneValido(celular)) {
            throw new RegraNegocioException("Celular inválido.", "Use o formato (xx) xxxxx-xxxx ou (xx) xxxx-xxxx.");
        }
        this.nomeCompleto = nomeCompleto.trim();
        this.email = email.trim().toLowerCase();
        this.celular = celular.trim();
        this.perfil = perfil;
        this.ativo = ativo;
    }

    public void alterarAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public void redefinirSenhaHash(String novoHash) {
        this.senhaHash = novoHash;
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
    }

    public String getCpf() { return cpf; }
    public String getCpfMascarado() { return CpfUtils.mascararCpf(cpf); }
    public String getNomeCompleto() { return nomeCompleto; }
    public String getEmail() { return email; }
    public String getCelular() { return celular; }
    public String getSenhaHash() { return senhaHash; }
    public PerfilUsuario getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public short getTentativasFalhas() { return tentativasFalhas; }
    public OffsetDateTime getBloqueadoAte() { return bloqueadoAte; }
    public OffsetDateTime getAtualizadoEm() { return atualizadoEm; }
}
