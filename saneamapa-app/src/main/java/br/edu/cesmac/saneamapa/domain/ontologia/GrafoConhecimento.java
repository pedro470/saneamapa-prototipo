package br.edu.cesmac.saneamapa.domain.ontologia;

import br.edu.cesmac.saneamapa.domain.exception.RegraNegocioException;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo de conhecimento tipado pela ontologia: cada nó é um indivíduo de uma {@link ClasseOntologia}
 * e cada aresta é uma {@link RelacaoOntologia}. O grafo só aceita o que a ontologia permite, então
 * um vínculo fora do domínio/contradomínio, ou um nó de classe abstrata ou protegida pela LGPD,
 * é recusado na inserção.
 */
public class GrafoConhecimento {

    public record No(String id, ClasseOntologia classe, String rotulo, Map<String, Object> atributos) {}

    public record Aresta(String origem, RelacaoOntologia relacao, String destino) {}

    private final Map<String, No> nos = new LinkedHashMap<>();
    private final Set<Aresta> arestas = new LinkedHashSet<>();

    /** Identificador estável do nó: classe em minúsculas + chave do indivíduo (ex.: "ponto:12"). */
    public static String idDe(ClasseOntologia classe, Object chave) {
        return classe.name().toLowerCase() + ":" + chave;
    }

    /** Adiciona o indivíduo, ou devolve o que já existe com a mesma classe e chave. */
    public No adicionarNo(ClasseOntologia classe, Object chave, String rotulo, Map<String, Object> atributos) {
        validarInstanciacao(classe);
        String id = idDe(classe, chave);
        return nos.computeIfAbsent(id, k -> new No(k, classe, rotulo,
                atributos == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(atributos))));
    }

    /** Atualiza rótulo e atributos de um nó existente (preservando arestas), ou cria um novo. */
    public No atualizarOuAdicionarNo(ClasseOntologia classe, Object chave, String rotulo, Map<String, Object> atributos) {
        validarInstanciacao(classe);
        String id = idDe(classe, chave);
        No novo = new No(id, classe, rotulo,
                atributos == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(atributos)));
        nos.put(id, novo);
        return novo;
    }

    /** Remove o nó e todas as arestas ligadas a ele. */
    public boolean removerNo(String id) {
        boolean existia = nos.remove(id) != null;
        if (existia) {
            arestas.removeIf(a -> a.origem().equals(id) || a.destino().equals(id));
        }
        return existia;
    }

    public void removerArestasDeOrigem(String origemId, RelacaoOntologia relacao) {
        arestas.removeIf(a -> a.origem().equals(origemId) && a.relacao() == relacao);
    }

    public void removerArestasDeDestino(String destinoId, RelacaoOntologia relacao) {
        arestas.removeIf(a -> a.destino().equals(destinoId) && a.relacao() == relacao);
    }

    public void removerArestasComNo(String noId, RelacaoOntologia relacao) {
        arestas.removeIf(a -> (a.origem().equals(noId) || a.destino().equals(noId)) && a.relacao() == relacao);
    }

    public List<No> nosDaClasse(ClasseOntologia classe) {
        return nos.values().stream().filter(n -> n.classe() == classe).toList();
    }

    public boolean temArestaComNo(String noId) {
        return arestas.stream().anyMatch(a -> a.origem().equals(noId) || a.destino().equals(noId));
    }

    private static void validarInstanciacao(ClasseOntologia classe) {
        if (!classe.podeSerInstanciada()) {
            throw new RegraNegocioException(
                    "A classe " + classe.getRotulo() + " não pode ter indivíduos no grafo.",
                    classe.isProtegidaLgpd()
                            ? "Dados pessoais só são consultados pelo acesso auditado do painel."
                            : "Use uma das subclasses concretas dessa classe.");
        }
    }

    /** Liga dois nós existentes, validando domínio e contradomínio da relação. Arestas repetidas são ignoradas. */
    public Aresta relacionar(String origemId, RelacaoOntologia relacao, String destinoId) {
        No origem = nos.get(origemId);
        No destino = nos.get(destinoId);
        if (origem == null || destino == null) {
            throw new RegraNegocioException(
                    "Relação \"" + relacao.getRotulo() + "\" aponta para um nó que não está no grafo.",
                    "Adicione os dois nós antes de relacioná-los.");
        }
        if (!relacao.podeSerMaterializada() || !relacao.aceita(origem.classe(), destino.classe())) {
            throw new RegraNegocioException(
                    String.format("A ontologia não permite \"%s %s %s\".",
                            origem.classe().getRotulo(), relacao.getRotulo(), destino.classe().getRotulo()),
                    String.format("A relação liga %s a %s.",
                            relacao.getDominio().getRotulo(), relacao.getContradominio().getRotulo()));
        }
        Aresta aresta = new Aresta(origemId, relacao, destinoId);
        arestas.add(aresta);
        return aresta;
    }

    public boolean contemNo(String id) {
        return nos.containsKey(id);
    }

    public List<No> nos() {
        return List.copyOf(nos.values());
    }

    public List<Aresta> arestas() {
        return List.copyOf(arestas);
    }

    /**
     * Subgrafo com tudo que está a até {@code saltos} arestas do nó central, em qualquer direção.
     * Devolve um grafo vazio se o nó não existir.
     */
    public GrafoConhecimento vizinhanca(String centroId, int saltos) {
        GrafoConhecimento sub = new GrafoConhecimento();
        if (!nos.containsKey(centroId)) {
            return sub;
        }
        Map<String, Set<String>> adjacencia = new LinkedHashMap<>();
        for (Aresta a : arestas) {
            adjacencia.computeIfAbsent(a.origem(), k -> new LinkedHashSet<>()).add(a.destino());
            adjacencia.computeIfAbsent(a.destino(), k -> new LinkedHashSet<>()).add(a.origem());
        }
        Map<String, Integer> distancia = new LinkedHashMap<>();
        Deque<String> fila = new ArrayDeque<>();
        distancia.put(centroId, 0);
        fila.add(centroId);
        while (!fila.isEmpty()) {
            String atual = fila.poll();
            int d = distancia.get(atual);
            if (d >= saltos) {
                continue;
            }
            for (String vizinho : adjacencia.getOrDefault(atual, Set.of())) {
                if (distancia.putIfAbsent(vizinho, d + 1) == null) {
                    fila.add(vizinho);
                }
            }
        }
        for (String id : distancia.keySet()) {
            sub.nos.put(id, nos.get(id));
        }
        for (Aresta a : arestas) {
            if (distancia.containsKey(a.origem()) && distancia.containsKey(a.destino())) {
                sub.arestas.add(a);
            }
        }
        return sub;
    }
}
