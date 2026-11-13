package br.edu.cesmac.saneamapa.domain.enums;

public enum PerfilUsuario {
    FISCAL("Fiscal"),
    PREFEITURA("Prefeitura");

    private final String rotulo;

    PerfilUsuario(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
