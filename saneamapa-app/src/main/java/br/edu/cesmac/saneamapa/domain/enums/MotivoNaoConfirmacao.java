package br.edu.cesmac.saneamapa.domain.enums;

/**
 * Motivo informado pelo fiscal quando a vistoria não confirma o ponto (RF007, nota 5; TBD-26).
 * FORA_DO_ESCOPO registra um problema real que não é de saneamento (poço, vazamento, lixo),
 * para a Prefeitura encaminhar por outros meios.
 */
public enum MotivoNaoConfirmacao {
    NAO_ENCONTRADO("Problema não encontrado no local"),
    FORA_DO_ESCOPO("Problema existe, mas não é de saneamento");

    private final String rotulo;

    MotivoNaoConfirmacao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
