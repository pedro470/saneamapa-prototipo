package br.edu.cesmac.saneamapa;

import com.fasterxml.jackson.databind.JsonNode;
import br.edu.cesmac.saneamapa.service.PrazoGuardaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaneaMapaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrazoGuardaService prazoGuardaService;

    private String obterToken(String email, String senha) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void fluxoCompletoMoradorAgrupamentoPrefeituraEFiscal() throws Exception {
        // 1. Morador registra 1º relato real -> cria novo ponto
        String primeiroRelato = """
                {
                  "tipoOcorrencia": "ESGOTO_CEU_ABERTO",
                  "descricao": "Esgoto vazando na esquina",
                  "nomeMorador": "Morador Teste",
                  "telefoneMorador": "(82) 99999-1234",
                  "latitude": -9.64410,
                  "longitude": -35.71555,
                  "precisaoMetros": 8,
                  "logradouro": "Rua do Arame",
                  "bairro": "Jacintinho",
                  "posicaoManual": false,
                  "evidencias": [
                    { "tipo": "FOTO", "origem": "CAMERA", "arquivoCaminho": "/api/midias/ev_foto1.jpg" }
                  ]
                }
                """;
        MvcResult criacaoRes = mockMvc.perform(post("/api/morador/relatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primeiroRelato))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agrupadoEmPontoExistente", is(false)))
                .andReturn();
        String codigoCriado = objectMapper.readTree(criacaoRes.getResponse().getContentAsString())
                .get("codigoPonto").asText();

        // 2. Segundo morador registra relato próximo (<= 50 m) -> agrupa no mesmo ponto
        String segundoRelato = """
                {
                  "tipoOcorrencia": "ESGOTO_CEU_ABERTO",
                  "descricao": "Esgoto vazando novamente na rua do arame",
                  "nomeMorador": "Segundo Morador",
                  "telefoneMorador": "(82) 98888-4321",
                  "latitude": -9.64412,
                  "longitude": -35.71556,
                  "precisaoMetros": 10,
                  "logradouro": "Rua do Arame",
                  "bairro": "Jacintinho",
                  "posicaoManual": false,
                  "evidencias": [
                    { "tipo": "FOTO", "origem": "CAMERA", "arquivoCaminho": "/api/midias/ev_foto2.jpg" }
                  ]
                }
                """;
        mockMvc.perform(post("/api/morador/relatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundoRelato))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoPonto", is(codigoCriado)))
                .andExpect(jsonPath("$.agrupadoEmPontoExistente", is(true)))
                .andExpect(jsonPath("$.prioridadeReduzida").doesNotExist());

        // 2b. RF001 (nota 5): relato sem nome e telefone é recusado, porque não há envio anônimo
        mockMvc.perform(post("/api/morador/relatos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(segundoRelato
                                .replace("\"nomeMorador\": \"Segundo Morador\",", "")
                                .replace("\"telefoneMorador\": \"(82) 98888-4321\",", "")))
                .andExpect(status().is4xxClientError());

        // 3. Endpoint público LGPD não expõe coordenadas nem urgência
        mockMvc.perform(get("/api/publico/pontos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latitudeAproximada").doesNotExist());

        // 4. Autenticação Prefeitura e Fiscal + controle RBAC
        String tokenPref = obterToken("ana.lima@exemplo.gov.br", "Sanea2026");
        String tokenFiscal = obterToken("carlos.mendes@exemplo.gov.br", "Sanea2026");

        mockMvc.perform(get("/api/prefeitura/auditoria").header("Authorization", "Bearer " + tokenFiscal))
                .andExpect(status().isForbidden());

        // 5. Prefeitura consulta indicadores e atribui o ponto real criado ao Fiscal
        mockMvc.perform(get("/api/prefeitura/indicadores").header("Authorization", "Bearer " + tokenPref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPontos", greaterThanOrEqualTo(1)));

        MvcResult pontosPref = mockMvc.perform(get("/api/prefeitura/pontos").header("Authorization", "Bearer " + tokenPref))
                .andExpect(status().isOk())
                .andReturn();
        Long pontoId = objectMapper.readTree(pontosPref.getResponse().getContentAsString()).get(0).get("id").asLong();

        MvcResult fiscaisRes = mockMvc.perform(get("/api/prefeitura/fiscais-ativos").header("Authorization", "Bearer " + tokenPref))
                .andExpect(status().isOk())
                .andReturn();
        Long fiscalId = objectMapper.readTree(fiscaisRes.getResponse().getContentAsString()).get(0).get("id").asLong();

        mockMvc.perform(put("/api/prefeitura/pontos/" + pontoId + "/atribuicao")
                        .header("Authorization", "Bearer " + tokenPref)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fiscalId\":" + fiscalId + "}"))
                .andExpect(status().isOk());

        // 6. Fiscal vê o ponto atribuído com coordenada exata e tenta vistoria fora do raio (>100m -> 422)
        mockMvc.perform(get("/api/fiscal/pontos")
                        .header("Authorization", "Bearer " + tokenFiscal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(pontoId.intValue())));

        mockMvc.perform(get("/api/fiscal/pontos/" + pontoId)
                        .header("Authorization", "Bearer " + tokenFiscal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relatos[0].logradouro", is("Rua do Arame")));

        String vistoriaLonge = """
                {
                  "resultado": "CONFIRMADO",
                  "latitudeFiscal": -9.7000,
                  "longitudeFiscal": -35.7800,
                  "midiasCamera": ["/api/midias/cam1.jpg"],
                  "origensMidia": ["CAMERA"]
                }
                """;
        mockMvc.perform(post("/api/fiscal/pontos/" + pontoId + "/vistoria")
                        .header("Authorization", "Bearer " + tokenFiscal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vistoriaLonge))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro", containsString("100 m")));

        // 7. Fiscal registra vistoria válida dentro do raio de 100 m -> CONFIRMADO
        String vistoriaValida = """
                {
                  "resultado": "CONFIRMADO",
                  "latitudeFiscal": -9.64411,
                  "longitudeFiscal": -35.71555,
                  "observacoes": "Esgoto confirmado no local",
                  "midiasCamera": ["/api/midias/cam_vistoria.jpg"],
                  "origensMidia": ["CAMERA"]
                }
                """;
        mockMvc.perform(post("/api/fiscal/pontos/" + pontoId + "/vistoria")
                        .header("Authorization", "Bearer " + tokenFiscal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vistoriaValida))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.novoStatus", is("Confirmado")));

        // 7b. RNF20 (TBD-29): antes de 180 dias da vistoria nada é eliminado; depois, nome e telefone saem uma vez só.
        org.junit.jupiter.api.Assertions.assertEquals(0,
                prazoGuardaService.eliminarDadosVencidos(java.time.OffsetDateTime.now().plusDays(179)));
        int anonimizados = prazoGuardaService.eliminarDadosVencidos(java.time.OffsetDateTime.now().plusDays(181));
        org.junit.jupiter.api.Assertions.assertTrue(anonimizados > 0);
        org.junit.jupiter.api.Assertions.assertEquals(0,
                prazoGuardaService.eliminarDadosVencidos(java.time.OffsetDateTime.now().plusDays(182)));

        // 8. Prefeitura verifica log de auditoria registrado
        mockMvc.perform(get("/api/prefeitura/auditoria")
                        .header("Authorization", "Bearer " + tokenPref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[*].tipoEvento", hasItems("MUDANCA_STATUS_PONTO", "ELIMINACAO_DADOS_MORADOR")));

        // 8. Prefeitura valida erro de CPF duplicado no cadastro de usuário (RF013)
        String usuarioCpfDuplicado = """
                {
                  "cpf": "218.334.001-00",
                  "nomeCompleto": "Outro Nome",
                  "email": "outro@exemplo.gov.br",
                  "celular": "(82) 99999-1111",
                  "senha": "Senha123",
                  "perfil": "FISCAL",
                  "ativo": true
                }
                """;
        mockMvc.perform(post("/api/prefeitura/usuarios")
                        .header("Authorization", "Bearer " + tokenPref)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(usuarioCpfDuplicado))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro", containsString("CPF já cadastrado")));

        // 9. Prefeitura importa CSV do Censo com erro de código de 14 dígitos e verifica rejeição (RF020 / RS013)
        MockMultipartFile csvInvalido = new MockMultipartFile(
                "arquivo", "censo_errado.csv", "text/csv",
                "codigo_setor;bairro;pct_agua;pct_esgoto\n27043040500012;Vergel;95;80\n".getBytes());
        mockMvc.perform(multipart("/api/prefeitura/censo/importar")
                        .file(csvInvalido)
                        .header("Authorization", "Bearer " + tokenPref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso", is(false)))
                .andExpect(jsonPath("$.erros[0]", containsString("15")));
    }

    @Test
    void uploadRealDeMidiasEConsultaConfiguracao() throws Exception {
        mockMvc.perform(get("/api/publico/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cidadePadrao", is("Maceió, AL")));

        // RNF19 (TBD-30): o termo de uso lê controlador, encarregado e prazo de guarda da instalação.
        mockMvc.perform(get("/api/publico/privacidade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prazoGuardaDias", is(180)))
                .andExpect(jsonPath("$.controladorNome", not(emptyString())))
                .andExpect(jsonPath("$.encarregadoContato", not(emptyString())));

        MockMultipartFile foto = new MockMultipartFile(
                "arquivo", "esgoto.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});
        MvcResult uploadRes = mockMvc.perform(multipart("/api/morador/midias").file(foto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.arquivoCaminho", startsWith("/api/midias/")))
                .andExpect(jsonPath("$.tipo", is("FOTO")))
                .andReturn();

        String caminho = objectMapper.readTree(uploadRes.getResponse().getContentAsString())
                .get("arquivoCaminho").asText();

        // Vídeo acima de 30 segundos deve ser recusado
        MockMultipartFile videoLongo = new MockMultipartFile(
                "arquivo", "longo.mp4", "video/mp4", new byte[]{5, 6, 7});
        mockMvc.perform(multipart("/api/morador/midias")
                        .file(videoLongo)
                        .param("duracaoSegundos", "45"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro", containsString("30 segundos")));

        // Acesso público à mídia salva é negado (proteção LGPD) e permitido para equipe autenticada
        mockMvc.perform(get(caminho))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.orientacao").exists());

        String tokenPref = obterToken("ana.lima@exemplo.gov.br", "Sanea2026");
        mockMvc.perform(get(caminho).param("token", tokenPref))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }
}
