package br.edu.cesmac.saneamapa.domain.enums;

public enum ResultadoVistoria {
    CONFIRMADO("Confirmado", StatusPonto.CONFIRMADO),
    NAO_CONFIRMADO("Não confirmado", StatusPonto.NAO_CONFIRMADO);

    private final String rotulo;
    private final StatusPonto statusResultante;

    ResultadoVistoria(String rotulo, StatusPonto statusResultante) {
        this.rotulo = rotulo;
        this.statusResultante = statusResultante;
    }

    public String getRotulo() {
        return rotulo;
    }

    public StatusPonto getStatusResultante() {
        return statusResultante;
    }
}
