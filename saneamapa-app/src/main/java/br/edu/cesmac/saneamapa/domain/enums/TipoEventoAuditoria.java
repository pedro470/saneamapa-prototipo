package br.edu.cesmac.saneamapa.domain.enums;

public enum TipoEventoAuditoria {
    ACESSO_CONTATO_MORADOR("Acesso a nome e telefone"),
    ACESSO_COORDENADA_EXATA("Acesso à coordenada exata"),
    AUDIO_OUVIDO("Áudio ouvido"),
    EDICAO_FOTO("Edição de foto"),
    CORRECAO_TRANSCRICAO("Correção de transcrição"),
    ATRIBUICAO_PONTO("Atribuição de ponto"),
    RETIRADA_ATRIBUICAO("Retirada de atribuição"),
    MUDANCA_STATUS_PONTO("Mudança de status"),
    ALTERACAO_USUARIO("Alteração de usuário"),
    IMPORTACAO_CENSO("Importação do Censo"),
    ROLLBACK_CENSO("Retorno à importação anterior do Censo"),
    ELIMINACAO_DADOS_MORADOR("Eliminação de nome e telefone");

    private final String rotulo;

    TipoEventoAuditoria(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
