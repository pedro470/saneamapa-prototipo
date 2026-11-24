package br.edu.cesmac.saneamapa.domain.ontologia;

/**
 * Relações (propriedades de objeto) da ontologia, cada uma com domínio e contradomínio.
 *
 * Uma relação declarada vem de um vínculo gravado no banco (chave estrangeira ou atributo).
 * Uma relação inferida não está gravada em lugar nenhum: é deduzida das geometrias na hora de
 * montar o grafo (ponto dentro de polígono, pontos próximos).
 */
public enum RelacaoOntologia {

    AGRUPADO_EM("agrupado em", ClasseOntologia.RELATO, ClasseOntologia.PONTO, false,
            "O relato foi agrupado neste ponto (mesmo tipo, até 50 m)."),
    COMPROVA("comprova", ClasseOntologia.EVIDENCIA, ClasseOntologia.RELATO, false,
            "A evidência foi anexada a este relato."),
    REGISTRADO_POR("registrado por", ClasseOntologia.RELATO, ClasseOntologia.MORADOR, false,
            "Quem enviou o relato. Não aparece no grafo: o contato só sai pelo acesso auditado."),
    VERIFICA("verifica", ClasseOntologia.VISTORIA, ClasseOntologia.PONTO, false,
            "A vistoria foi feita neste ponto."),
    REALIZADA_POR("realizada por", ClasseOntologia.VISTORIA, ClasseOntologia.FISCAL, false,
            "Fiscal que fez a vistoria em campo."),
    ATRIBUIDO_A("atribuído a", ClasseOntologia.PONTO, ClasseOntologia.FISCAL, false,
            "Fiscal responsável pelo ponto na fila de vistoria."),
    E_DO_TIPO("é do tipo", ClasseOntologia.PONTO, ClasseOntologia.TIPO_OCORRENCIA, false,
            "Classificação do problema relatado."),
    AFETA_SERVICO("afeta o serviço", ClasseOntologia.TIPO_OCORRENCIA, ClasseOntologia.SERVICO_SANEAMENTO, false,
            "Serviço de saneamento ao qual o tipo de ocorrência pertence."),
    LOCALIZADO_EM("localizado em", ClasseOntologia.PONTO, ClasseOntologia.BAIRRO, false,
            "Bairro do ponto."),
    PERTENCE_A("pertence a", ClasseOntologia.SETOR_CENSITARIO, ClasseOntologia.BAIRRO, false,
            "Bairro do setor censitário."),
    DENTRO_DO_SETOR("dentro do setor", ClasseOntologia.PONTO, ClasseOntologia.SETOR_CENSITARIO, true,
            "O ponto cai dentro do polígono do setor censitário."),
    SOB_INFLUENCIA_DE("sob influência de", ClasseOntologia.PONTO, ClasseOntologia.CAMADA_VULNERABILIDADE, true,
            "O ponto cai dentro da área de uma camada de vulnerabilidade ativa."),
    VIZINHO_DE("vizinho de", ClasseOntologia.PONTO, ClasseOntologia.PONTO, true,
            "Outro ponto a até 300 m: indica problemas concentrados na mesma área.");

    /** Distância máxima, em metros, para dois pontos serem considerados vizinhos. */
    public static final double RAIO_VIZINHANCA_METROS = 300.0;

    private final String rotulo;
    private final ClasseOntologia dominio;
    private final ClasseOntologia contradominio;
    private final boolean inferida;
    private final String descricao;

    RelacaoOntologia(String rotulo, ClasseOntologia dominio, ClasseOntologia contradominio, boolean inferida, String descricao) {
        this.rotulo = rotulo;
        this.dominio = dominio;
        this.contradominio = contradominio;
        this.inferida = inferida;
        this.descricao = descricao;
    }

    /** A relação aceita o par quando cada ponta é subclasse do que foi declarado. */
    public boolean aceita(ClasseOntologia origem, ClasseOntologia destino) {
        return origem.ehSubclasseDe(dominio) && destino.ehSubclasseDe(contradominio);
    }

    /** Relações que tocam uma classe protegida pela LGPD nunca são materializadas. */
    public boolean podeSerMaterializada() {
        return !dominio.isProtegidaLgpd() && !contradominio.isProtegidaLgpd();
    }

    public String getRotulo() { return rotulo; }
    public ClasseOntologia getDominio() { return dominio; }
    public ClasseOntologia getContradominio() { return contradominio; }
    public boolean isInferida() { return inferida; }
    public String getDescricao() { return descricao; }
}
