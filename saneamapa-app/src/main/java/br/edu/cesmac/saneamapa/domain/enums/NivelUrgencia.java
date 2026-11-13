package br.edu.cesmac.saneamapa.domain.enums;

public enum NivelUrgencia {
    NORMAL("Normal", 1),
    ELEVADA("Elevada", 2),
    CRITICA("Crítica", 3);

    private final String rotulo;
    private final int nivelMedidor;

    NivelUrgencia(String rotulo, int nivelMedidor) {
        this.rotulo = rotulo;
        this.nivelMedidor = nivelMedidor;
    }

    public String getRotulo() {
        return rotulo;
    }

    public int getNivelMedidor() {
        return nivelMedidor;
    }

    public static NivelUrgencia porPeso(int peso) {
        if (peso <= 0) {
            return NORMAL;
        }
        if (peso <= 2) {
            return ELEVADA;
        }
        return CRITICA;
    }
}
