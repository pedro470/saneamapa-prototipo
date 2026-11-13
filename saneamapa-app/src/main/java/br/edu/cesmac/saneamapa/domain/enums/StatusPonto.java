package br.edu.cesmac.saneamapa.domain.enums;

public enum StatusPonto {
    AGUARDANDO_VISTORIA("Aguardando vistoria"),
    EM_VISTORIA("Em vistoria"),
    CONFIRMADO("Confirmado"),
    NAO_CONFIRMADO("Não confirmado");

    private final String rotulo;

    StatusPonto(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean estaNaFila() {
        return this == AGUARDANDO_VISTORIA || this == EM_VISTORIA;
    }

    /**
     * Valida as transições permitidas conforme o Ciclo de Vida do Ponto (Seção 10.1).
     */
    public boolean podeTransicionarPara(StatusPonto destino) {
        if (this == destino) {
            return true;
        }
        return switch (this) {
            case AGUARDANDO_VISTORIA -> destino == EM_VISTORIA;
            case EM_VISTORIA -> destino == AGUARDANDO_VISTORIA || destino == CONFIRMADO || destino == NAO_CONFIRMADO;
            case NAO_CONFIRMADO -> destino == AGUARDANDO_VISTORIA;
            case CONFIRMADO -> false;
        };
    }
}
