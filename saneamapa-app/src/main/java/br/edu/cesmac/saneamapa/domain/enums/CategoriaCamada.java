package br.edu.cesmac.saneamapa.domain.enums;

public enum CategoriaCamada {
    PUBLICO_INFANTIL_ESCOLAR("Público infantil e escolar", false),
    IDOSOS_CUIDADOS("Idosos e cuidados", false),
    GESTANTES_RECEM_NASCIDOS("Gestantes e recém-nascidos", false),
    COMUNIDADES_TRADICIONAIS_VULNERAVEIS("Comunidades tradicionais e vulneráveis", false),
    VULNERABILIDADE_EPIDEMIOLOGICA("Vulnerabilidade epidemiológica", true);

    private final String rotulo;
    private final boolean exigePoligono;

    CategoriaCamada(String rotulo, boolean exigePoligono) {
        this.rotulo = rotulo;
        this.exigePoligono = exigePoligono;
    }

    public String getRotulo() {
        return rotulo;
    }

    public boolean isExigePoligono() {
        return exigePoligono;
    }
}
