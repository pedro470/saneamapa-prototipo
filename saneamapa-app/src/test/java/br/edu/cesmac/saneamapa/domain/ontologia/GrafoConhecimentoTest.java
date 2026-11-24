package br.edu.cesmac.saneamapa.domain.ontologia;

import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GrafoConhecimentoTest {

    private static String id(ClasseOntologia c, Object chave) {
        return GrafoConhecimento.idDe(c, chave);
    }

    @Test
    void subclasseHerdaDeTodosOsAncestrais() {
        assertTrue(ClasseOntologia.EVIDENCIA_FOTO.ehSubclasseDe(ClasseOntologia.EVIDENCIA));
        assertTrue(ClasseOntologia.EVIDENCIA_FOTO.ehSubclasseDe(ClasseOntologia.REGISTRO));
        assertTrue(ClasseOntologia.EVIDENCIA_FOTO.ehSubclasseDe(ClasseOntologia.ENTIDADE));
        assertTrue(ClasseOntologia.PONTO.ehSubclasseDe(ClasseOntologia.PONTO));
        assertFalse(ClasseOntologia.EVIDENCIA.ehSubclasseDe(ClasseOntologia.EVIDENCIA_FOTO));
        assertFalse(ClasseOntologia.FISCAL.ehSubclasseDe(ClasseOntologia.TERRITORIO));
    }

    @Test
    void todaClasseChegaNaRaizETodaRelacaoLigaClassesInstanciaveisOuProtegidas() {
        for (ClasseOntologia c : ClasseOntologia.values()) {
            assertTrue(c.ehSubclasseDe(ClasseOntologia.ENTIDADE), c + " deve descender de ENTIDADE");
        }
        for (RelacaoOntologia r : RelacaoOntologia.values()) {
            // Uma relação que não pode ser materializada só existe por tocar uma classe protegida pela LGPD.
            assertEquals(!r.getDominio().isProtegidaLgpd() && !r.getContradominio().isProtegidaLgpd(),
                    r.podeSerMaterializada(), r.name());
        }
    }

    @Test
    void relacaoAceitaSubclasseDoDominio() {
        GrafoConhecimento g = new GrafoConhecimento();
        g.adicionarNo(ClasseOntologia.RELATO, 1, "SM-1", null);
        g.adicionarNo(ClasseOntologia.EVIDENCIA_AUDIO, 7, "Áudio 7", null);

        // COMPROVA é declarada para EVIDENCIA; um áudio é uma evidência.
        g.relacionar(id(ClasseOntologia.EVIDENCIA_AUDIO, 7), RelacaoOntologia.COMPROVA, id(ClasseOntologia.RELATO, 1));
        assertEquals(1, g.arestas().size());
    }

    @Test
    void grafoRecusaRelacaoForaDoDominioOuContradominio() {
        GrafoConhecimento g = new GrafoConhecimento();
        g.adicionarNo(ClasseOntologia.PONTO, 1, "P-0001", null);
        g.adicionarNo(ClasseOntologia.RELATO, 1, "SM-1", null);
        g.adicionarNo(ClasseOntologia.BAIRRO, "vergel", "Vergel", null);

        // Sentido invertido: é o relato que é agrupado no ponto.
        assertThrows(RegraNegocioException.class, () ->
                g.relacionar(id(ClasseOntologia.PONTO, 1), RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.RELATO, 1)));
        // Contradomínio errado: relato não é atribuído a bairro.
        assertThrows(RegraNegocioException.class, () ->
                g.relacionar(id(ClasseOntologia.RELATO, 1), RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.BAIRRO, "vergel")));
        // Nó inexistente.
        assertThrows(RegraNegocioException.class, () ->
                g.relacionar(id(ClasseOntologia.RELATO, 99), RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.PONTO, 1)));
        assertTrue(g.arestas().isEmpty());
    }

    @Test
    void grafoNaoMaterializaClasseAbstrataNemDadoPessoal() {
        GrafoConhecimento g = new GrafoConhecimento();
        assertThrows(RegraNegocioException.class, () -> g.adicionarNo(ClasseOntologia.EVIDENCIA, 1, "Evidência", null));
        // LGPD: o morador está no vocabulário, mas nunca vira nó.
        assertThrows(RegraNegocioException.class, () -> g.adicionarNo(ClasseOntologia.MORADOR, 1, "Maria", null));
        assertFalse(RelacaoOntologia.REGISTRADO_POR.podeSerMaterializada());
        assertTrue(g.nos().isEmpty());
    }

    @Test
    void noEArestaRepetidosNaoDuplicam() {
        GrafoConhecimento g = new GrafoConhecimento();
        g.adicionarNo(ClasseOntologia.PONTO, 1, "P-0001", Map.of("Status", "Confirmado"));
        g.adicionarNo(ClasseOntologia.PONTO, 1, "outro rótulo", null);
        g.adicionarNo(ClasseOntologia.RELATO, 1, "SM-1", null);
        g.relacionar(id(ClasseOntologia.RELATO, 1), RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.PONTO, 1));
        g.relacionar(id(ClasseOntologia.RELATO, 1), RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.PONTO, 1));

        assertEquals(2, g.nos().size());
        assertEquals(1, g.arestas().size());
        assertEquals("P-0001", g.nos().get(0).rotulo());
    }

    @Test
    void vizinhancaRespeitaONumeroDeSaltosNasDuasDirecoes() {
        // evidência -> relato -> ponto -> bairro <- setor
        GrafoConhecimento g = new GrafoConhecimento();
        g.adicionarNo(ClasseOntologia.EVIDENCIA_FOTO, 1, "Foto 1", null);
        g.adicionarNo(ClasseOntologia.RELATO, 1, "SM-1", null);
        g.adicionarNo(ClasseOntologia.PONTO, 1, "P-0001", null);
        g.adicionarNo(ClasseOntologia.BAIRRO, "vergel", "Vergel", null);
        g.adicionarNo(ClasseOntologia.SETOR_CENSITARIO, 5, "Setor 5", null);
        g.adicionarNo(ClasseOntologia.PONTO, 2, "P-0002", null); // isolado
        g.relacionar(id(ClasseOntologia.EVIDENCIA_FOTO, 1), RelacaoOntologia.COMPROVA, id(ClasseOntologia.RELATO, 1));
        g.relacionar(id(ClasseOntologia.RELATO, 1), RelacaoOntologia.AGRUPADO_EM, id(ClasseOntologia.PONTO, 1));
        g.relacionar(id(ClasseOntologia.PONTO, 1), RelacaoOntologia.LOCALIZADO_EM, id(ClasseOntologia.BAIRRO, "vergel"));
        g.relacionar(id(ClasseOntologia.SETOR_CENSITARIO, 5), RelacaoOntologia.PERTENCE_A, id(ClasseOntologia.BAIRRO, "vergel"));

        GrafoConhecimento um = g.vizinhanca(id(ClasseOntologia.PONTO, 1), 1);
        assertEquals(Set.of("ponto:1", "relato:1", "bairro:vergel"), ids(um));
        assertEquals(2, um.arestas().size());

        GrafoConhecimento dois = g.vizinhanca(id(ClasseOntologia.PONTO, 1), 2);
        assertEquals(Set.of("ponto:1", "relato:1", "bairro:vergel", "evidencia_foto:1", "setor_censitario:5"), ids(dois));
        assertEquals(4, dois.arestas().size());

        assertTrue(g.vizinhanca("ponto:999", 2).nos().isEmpty());
    }

    @Test
    void atualizacaoIncrementalPreservaArestasELimpaRemocoes() {
        GrafoConhecimento g = new GrafoConhecimento();
        String p1 = id(ClasseOntologia.PONTO, 1);
        String p2 = id(ClasseOntologia.PONTO, 2);
        String f1 = id(ClasseOntologia.FISCAL, 10);
        String b1 = id(ClasseOntologia.BAIRRO, "vergel");

        g.adicionarNo(ClasseOntologia.PONTO, 1, "P-0001", Map.of("Status", "Aguardando vistoria"));
        g.adicionarNo(ClasseOntologia.PONTO, 2, "P-0002", Map.of("Status", "Aguardando vistoria"));
        g.adicionarNo(ClasseOntologia.FISCAL, 10, "Carlos", Map.of("Situação", "Ativo"));
        g.adicionarNo(ClasseOntologia.BAIRRO, "vergel", "Vergel", null);
        g.relacionar(p1, RelacaoOntologia.ATRIBUIDO_A, f1);
        g.relacionar(p1, RelacaoOntologia.LOCALIZADO_EM, b1);
        g.relacionar(p1, RelacaoOntologia.VIZINHO_DE, p2);

        g.atualizarOuAdicionarNo(ClasseOntologia.PONTO, 1, "P-0001", Map.of("Status", "Em vistoria"));
        assertEquals(3, g.arestas().size());
        assertEquals("Em vistoria", g.nos().stream().filter(n -> n.id().equals(p1)).findFirst().orElseThrow().atributos().get("Status"));

        g.removerArestasDeOrigem(p1, RelacaoOntologia.ATRIBUIDO_A);
        assertFalse(g.temArestaComNo(f1));
        assertTrue(g.removerNo(f1));
        assertFalse(g.removerNo(f1));

        g.removerArestasDeDestino(b1, RelacaoOntologia.LOCALIZADO_EM);
        assertFalse(g.temArestaComNo(b1));

        g.removerArestasComNo(p2, RelacaoOntologia.VIZINHO_DE);
        assertTrue(g.arestas().isEmpty());
    }

    private static Set<String> ids(GrafoConhecimento g) {
        return g.nos().stream().map(GrafoConhecimento.No::id).collect(Collectors.toSet());
    }
}
