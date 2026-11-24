package br.edu.cesmac.saneamapa.domain.ontologia;

/**
 * Classes (conceitos) da ontologia do SaneaMapa, organizadas numa hierarquia "é um".
 *
 * Classes abstratas só agrupam: nunca viram nó do grafo. Classes protegidas pela LGPD existem
 * no vocabulário (o morador faz parte do domínio), mas o grafo recusa instanciá-las, para que
 * nome e telefone nunca saiam do caminho auditado (RF002, RNF de privacidade).
 */
public enum ClasseOntologia {

    ENTIDADE("Entidade", null, true, false,
            "Raiz da hierarquia: tudo que o SaneaMapa conhece."),

    REGISTRO("Registro", ENTIDADE, true, false,
            "Fato registrado no sistema durante o ciclo de vida de uma ocorrência."),
    PONTO("Ponto", REGISTRO, false, false,
            "Local com problema de saneamento, que agrupa relatos do mesmo tipo num raio de 50 m (RF004)."),
    RELATO("Relato", REGISTRO, false, false,
            "Denúncia enviada por um morador (RF001)."),
    EVIDENCIA("Evidência", REGISTRO, true, false,
            "Mídia anexada a um relato, já com o tratamento de privacidade aplicado (RF003)."),
    EVIDENCIA_FOTO("Foto", EVIDENCIA, false, false, "Foto com desfoque de rostos e placas."),
    EVIDENCIA_VIDEO("Vídeo", EVIDENCIA, false, false, "Vídeo de até 30 s, com desfoque e sem som."),
    EVIDENCIA_AUDIO("Áudio", EVIDENCIA, false, false, "Áudio de até 60 s, com transcrição."),
    VISTORIA("Vistoria", REGISTRO, false, false,
            "Verificação em campo que confirma ou não o ponto (RF007)."),

    AGENTE("Agente", ENTIDADE, true, false,
            "Pessoa que atua sobre os registros."),
    FISCAL("Fiscal", AGENTE, false, false,
            "Usuário interno que vistoria os pontos atribuídos a ele."),
    MORADOR("Morador", AGENTE, false, true,
            "Autor do relato. Dado pessoal: existe no vocabulário, mas não é materializado no grafo."),

    TERRITORIO("Território", ENTIDADE, true, false,
            "Recorte espacial usado para localizar e priorizar pontos."),
    BAIRRO("Bairro", TERRITORIO, false, false, "Bairro informado nos relatos e nos setores do Censo."),
    SETOR_CENSITARIO("Setor censitário", TERRITORIO, false, false,
            "Setor do Censo do IBGE da importação em uso, com cobertura de água e esgoto (RF020)."),
    CAMADA_VULNERABILIDADE("Camada de vulnerabilidade", TERRITORIO, false, false,
            "Área de sensibilidade social que eleva a urgência dos pontos dentro dela (RF006)."),

    CONCEITO("Conceito", ENTIDADE, true, false,
            "Vocabulário controlado que classifica os registros."),
    TIPO_OCORRENCIA("Tipo de ocorrência", CONCEITO, false, false,
            "Um dos quatro problemas de saneamento que o morador pode relatar."),
    SERVICO_SANEAMENTO("Serviço de saneamento", CONCEITO, false, false,
            "Serviço público afetado: água ou esgoto.");

    private final String rotulo;
    private final ClasseOntologia superclasse;
    private final boolean abstrata;
    private final boolean protegidaLgpd;
    private final String descricao;

    ClasseOntologia(String rotulo, ClasseOntologia superclasse, boolean abstrata, boolean protegidaLgpd, String descricao) {
        this.rotulo = rotulo;
        this.superclasse = superclasse;
        this.abstrata = abstrata;
        this.protegidaLgpd = protegidaLgpd;
        this.descricao = descricao;
    }

    /** Subsunção: toda classe é subclasse de si mesma e de todos os seus ancestrais. */
    public boolean ehSubclasseDe(ClasseOntologia outra) {
        for (ClasseOntologia c = this; c != null; c = c.superclasse) {
            if (c == outra) {
                return true;
            }
        }
        return false;
    }

    /** Só classes concretas e não protegidas pela LGPD podem ter indivíduos no grafo. */
    public boolean podeSerInstanciada() {
        return !abstrata && !protegidaLgpd;
    }

    public String getRotulo() { return rotulo; }
    public ClasseOntologia getSuperclasse() { return superclasse; }
    public boolean isAbstrata() { return abstrata; }
    public boolean isProtegidaLgpd() { return protegidaLgpd; }
    public String getDescricao() { return descricao; }
}
