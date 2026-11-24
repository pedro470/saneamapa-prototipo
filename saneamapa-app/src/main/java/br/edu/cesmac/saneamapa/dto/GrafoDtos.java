package br.edu.cesmac.saneamapa.dto;

import java.util.List;
import java.util.Map;

public final class GrafoDtos {

    public record ClasseDto(
            String id,
            String rotulo,
            String superclasse,
            boolean abstrata,
            boolean protegidaLgpd,
            String descricao
    ) {}

    public record RelacaoDto(
            String id,
            String rotulo,
            String dominio,
            String contradominio,
            boolean inferida,
            boolean materializada,
            String descricao
    ) {}

    public record OntologiaDto(
            List<ClasseDto> classes,
            List<RelacaoDto> relacoes
    ) {}

    public record NoDto(
            String id,
            String classe,
            String rotulo,
            Map<String, Object> atributos
    ) {}

    public record ArestaDto(
            String origem,
            String relacao,
            String rotulo,
            String destino,
            boolean inferida
    ) {}

    public record GrafoDto(
            String centro,
            List<NoDto> nos,
            List<ArestaDto> arestas
    ) {}

    private GrafoDtos() {
    }
}
