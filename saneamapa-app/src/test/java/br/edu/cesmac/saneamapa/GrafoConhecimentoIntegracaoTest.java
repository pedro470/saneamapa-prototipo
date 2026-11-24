package br.edu.cesmac.saneamapa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// O fluxo completo de SaneaMapaIntegracaoTest parte de um banco sem pontos: este teste descarta o contexto ao terminar.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GrafoConhecimentoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String obterToken(String email) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"Sanea2026\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private static List<String> relacoesDe(JsonNode grafo, String origem) {
        List<String> saida = new ArrayList<>();
        for (JsonNode a : grafo.get("arestas")) {
            if (a.get("origem").asText().equals(origem)) {
                saida.add(a.get("relacao").asText());
            }
        }
        return saida;
    }

    @Test
    void grafoLigaORelatoAoTerritorioSemExporDadosPessoais() throws Exception {
        // Relato ao lado da creche do Vergel: dentro do setor censitário, da camada da creche
        // e do polígono epidemiológico carregados pelo DataInitializer.
        String relato = """
                {
                  "tipoOcorrencia": "RUA_SEM_ESGOTO",
                  "descricao": "Rua inteira sem rede",
                  "nomeMorador": "Zuleide Grafo Teste",
                  "telefoneMorador": "(82) 97777-0011",
                  "latitude": -9.67130,
                  "longitude": -35.74900,
                  "precisaoMetros": 6,
                  "logradouro": "Rua da Creche",
                  "bairro": "Vergel do Lago",
                  "posicaoManual": false,
                  "evidencias": [
                    { "tipo": "FOTO", "origem": "CAMERA", "arquivoCaminho": "/api/midias/grafo_foto.jpg" }
                  ]
                }
                """;
        MvcResult criado = mockMvc.perform(post("/api/morador/relatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relato))
                .andExpect(status().isCreated())
                .andReturn();
        String codigoPonto = objectMapper.readTree(criado.getResponse().getContentAsString()).get("codigoPonto").asText();

        // RBAC: o grafo é da Prefeitura.
        mockMvc.perform(get("/api/prefeitura/grafo")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/prefeitura/grafo")
                        .header("Authorization", "Bearer " + obterToken("carlos.mendes@exemplo.gov.br")))
                .andExpect(status().isForbidden());

        String token = obterToken("ana.lima@exemplo.gov.br");

        // Ontologia: o morador existe como classe protegida e a relação dele fica fora do grafo.
        mockMvc.perform(get("/api/prefeitura/ontologia").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classes[?(@.id=='MORADOR')].protegidaLgpd").value(true))
                .andExpect(jsonPath("$.classes[?(@.id=='EVIDENCIA_FOTO')].superclasse").value("EVIDENCIA"))
                .andExpect(jsonPath("$.relacoes[?(@.id=='REGISTRADO_POR')].materializada").value(false))
                .andExpect(jsonPath("$.relacoes[?(@.id=='DENTRO_DO_SETOR')].inferida").value(true));

        String corpo = mockMvc.perform(get("/api/prefeitura/grafo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // LGPD: nada de nome, telefone ou coordenada do relato no grafo.
        assertFalse(corpo.contains("Zuleide"), "nome do morador não pode aparecer no grafo");
        assertFalse(corpo.contains("97777-0011"), "telefone do morador não pode aparecer no grafo");
        assertFalse(corpo.contains("9.6713"), "coordenada exata não pode aparecer no grafo");
        assertFalse(corpo.contains("\"classe\":\"MORADOR\""));

        JsonNode grafo = objectMapper.readTree(corpo);
        String pontoNoId = null;
        long pontoId = -1;
        for (JsonNode n : grafo.get("nos")) {
            if (n.get("classe").asText().equals("PONTO") && n.get("rotulo").asText().equals(codigoPonto)) {
                pontoNoId = n.get("id").asText();
                pontoId = n.get("atributos").get("_pontoId").asLong();
            }
        }
        assertNotNull(pontoNoId, "o ponto criado deve ser um nó do grafo");

        List<String> relacoes = relacoesDe(grafo, pontoNoId);
        assertTrue(relacoes.contains("E_DO_TIPO"));
        assertTrue(relacoes.contains("LOCALIZADO_EM"));
        // Inferidas das geometrias, sem vínculo gravado no banco.
        assertTrue(relacoes.contains("DENTRO_DO_SETOR"), "ponto dentro do polígono do setor do Vergel");
        assertTrue(relacoes.stream().filter("SOB_INFLUENCIA_DE"::equals).count() >= 2,
                "ponto dentro da camada da creche e do polígono epidemiológico");

        // Vizinhança de 1 salto: o ponto, o relato dele e o que está ligado direto; a evidência fica a 2 saltos.
        JsonNode um = objectMapper.readTree(mockMvc.perform(get("/api/prefeitura/grafo")
                        .param("pontoId", String.valueOf(pontoId)).param("saltos", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.centro").value(pontoNoId))
                .andReturn().getResponse().getContentAsString());
        List<String> classesUm = new ArrayList<>();
        um.get("nos").forEach(n -> classesUm.add(n.get("classe").asText()));
        assertTrue(classesUm.contains("RELATO"));
        assertFalse(classesUm.contains("EVIDENCIA_FOTO"));

        mockMvc.perform(get("/api/prefeitura/grafo")
                        .param("pontoId", String.valueOf(pontoId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nos[?(@.classe=='EVIDENCIA_FOTO')]").isNotEmpty());

        mockMvc.perform(get("/api/prefeitura/grafo").param("pontoId", "999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
