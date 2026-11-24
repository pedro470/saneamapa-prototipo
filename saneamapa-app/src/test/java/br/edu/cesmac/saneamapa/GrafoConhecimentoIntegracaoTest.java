package br.edu.cesmac.saneamapa;

import br.edu.cesmac.saneamapa.domain.ontologia.GrafoConhecimento;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.ArestaDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.GrafoDto;
import br.edu.cesmac.saneamapa.dto.GrafoDtos.NoDto;
import br.edu.cesmac.saneamapa.service.GrafoConhecimentoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    @Autowired
    private GrafoConhecimentoService grafoService;

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
        assertEquals(1, grafoService.getQuantidadeMontagensCompletas(),
                "o grafo deve ser montado uma única vez na subida da aplicação");

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

        // Segundo ponto a ~110 m do primeiro para exercitar a inferência incremental VIZINHO_DE
        String relatoVizinho = """
                {
                  "tipoOcorrencia": "CASA_SEM_AGUA",
                  "descricao": "Falta água na rua vizinha",
                  "nomeMorador": "Morador Vizinho",
                  "telefoneMorador": "(82) 96666-0022",
                  "latitude": -9.67230,
                  "longitude": -35.74900,
                  "precisaoMetros": 6,
                  "logradouro": "Rua Paralela",
                  "bairro": "Vergel do Lago",
                  "posicaoManual": false,
                  "evidencias": [
                    { "tipo": "FOTO", "origem": "CAMERA", "arquivoCaminho": "/api/midias/viz_foto.jpg" }
                  ]
                }
                """;
        mockMvc.perform(post("/api/morador/relatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(relatoVizinho))
                .andExpect(status().isCreated());

        // Atribui fiscal ao primeiro ponto, registra vistoria, alterna camada, importa Censo e atualiza fiscal
        String tokenFiscal = obterToken("carlos.mendes@exemplo.gov.br");
        JsonNode fiscais = objectMapper.readTree(mockMvc.perform(get("/api/prefeitura/fiscais-ativos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long fiscalId = fiscais.get(0).get("id").asLong();

        mockMvc.perform(put("/api/prefeitura/pontos/" + pontoId + "/atribuicao")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fiscalId\":" + fiscalId + "}"))
                .andExpect(status().isOk());

        String vistoria = """
                {
                  "resultado": "CONFIRMADO",
                  "latitudeFiscal": -9.67131,
                  "longitudeFiscal": -35.74900,
                  "observacoes": "Confirmado em campo",
                  "midiasCamera": ["/api/midias/vist_cam.jpg"],
                  "origensMidia": ["CAMERA"]
                }
                """;
        mockMvc.perform(post("/api/fiscal/pontos/" + pontoId + "/vistoria")
                        .header("Authorization", "Bearer " + tokenFiscal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vistoria))
                .andExpect(status().isOk());

        JsonNode camadas = objectMapper.readTree(mockMvc.perform(get("/api/prefeitura/camadas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        long camadaId = camadas.get(0).get("id").asLong();
        mockMvc.perform(patch("/api/prefeitura/camadas/" + camadaId + "/alternar-ativa")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MockMultipartFile csvCenso = new MockMultipartFile(
                "arquivo", "censo_incremental.csv", "text/csv",
                "codigo_setor;bairro;pct_agua;pct_esgoto;poligono\n270430405000123;Vergel do Lago;98;90;[[-9.675,-35.755],[-9.665,-35.755],[-9.665,-35.742],[-9.675,-35.742]]\n".getBytes());
        mockMvc.perform(multipart("/api/prefeitura/censo/importar")
                        .file(csvCenso)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true));

        String atualizaFiscal = """
                {
                  "nomeCompleto": "Carlos Mendes Atualizado",
                  "email": "carlos.mendes@exemplo.gov.br",
                  "celular": "(82) 99876-1020",
                  "perfil": "FISCAL",
                  "ativo": true
                }
                """;
        mockMvc.perform(put("/api/prefeitura/usuarios/" + fiscalId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizaFiscal))
                .andExpect(status().isOk());

        // Consultas repetidas não disparam reconstrução completa
        mockMvc.perform(get("/api/prefeitura/grafo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertEquals(1, grafoService.getQuantidadeMontagensCompletas(),
                "leituras e mutações incrementais não devem reconstruir o grafo inteiro");

        // Equivalência exata entre o grafo mantido incrementalmente e uma montagem limpa do banco
        GrafoDto incremental = grafoService.obterGrafo(null, null);
        GrafoConhecimento limpo = grafoService.montar();
        Set<NoDto> nosInc = Set.copyOf(incremental.nos());
        Set<NoDto> nosLimpo = limpo.nos().stream()
                .map(n -> new NoDto(n.id(), n.classe().name(), n.rotulo(), n.atributos()))
                .collect(Collectors.toSet());
        Set<ArestaDto> arestasInc = Set.copyOf(incremental.arestas());
        Set<ArestaDto> arestasLimpo = limpo.arestas().stream()
                .map(a -> new ArestaDto(a.origem(), a.relacao().name(), a.relacao().getRotulo(), a.destino(), a.relacao().isInferida()))
                .collect(Collectors.toSet());

        assertEquals(nosLimpo, nosInc, "nós do grafo incremental devem bater com montagem do zero");
        assertEquals(arestasLimpo, arestasInc, "arestas do grafo incremental devem bater com montagem do zero");
    }
}
