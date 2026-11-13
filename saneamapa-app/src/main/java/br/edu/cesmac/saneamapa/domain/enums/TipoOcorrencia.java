package br.edu.cesmac.saneamapa.domain.enums;

public enum TipoOcorrencia {
    CASA_SEM_AGUA("Casa sem ligação de água", "AGUA"),
    AGUA_FORA_PADRAO("Água fora do padrão", "AGUA"),
    RUA_SEM_ESGOTO("Rua sem rede de esgoto", "ESGOTO"),
    ESGOTO_CEU_ABERTO("Esgoto a céu aberto", "ESGOTO");

    private final String rotulo;
    private final String servicoCenso;

    TipoOcorrencia(String rotulo, String servicoCenso) {
        this.rotulo = rotulo;
        this.servicoCenso = servicoCenso;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getServicoCenso() {
        return servicoCenso;
    }
}
