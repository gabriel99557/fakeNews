package com.example.fakeNews;

import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.entity.Analise;
import com.example.fakeNews.entity.Usuario;
import com.example.fakeNews.repository.AnaliseRepository;
import com.example.fakeNews.repository.UsuarioRepository;
import com.example.fakeNews.service.HistoricoService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Histórico pessoal (RF03), validação e anotações (Seção IV.H) contra um banco H2 em memória. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoricoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnaliseRepository analiseRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistoricoService historicoService;

    private String tokenAna;
    private Usuario ana;
    private String tokenBruno;

    @BeforeEach
    void preparar() throws Exception {
        analiseRepository.deleteAll();
        usuarioRepository.deleteAll();
        tokenAna = cadastrar("ana@exemplo.com");
        tokenBruno = cadastrar("bruno@exemplo.com");
        ana = usuarioRepository.findByEmail("ana@exemplo.com").orElseThrow();
    }

    // ---------- gravação assíncrona ----------

    @Test
    void salvarAnaliseAsyncPersisteComDonoEMesmoIdDaResposta() throws Exception {
        AnaliseResponse resultado = new AnaliseResponse();
        resultado.setId(UUID.randomUUID());
        resultado.setVeredito("FALSO");
        resultado.setProbabilidadeFake(87);
        resultado.setJustificativa("Linguagem sensacionalista e ausência de fontes.");
        resultado.setIndicadores(List.of("uso de caixa alta", "apelo emocional"));
        resultado.setOrigem("texto");

        historicoService.salvarAnaliseAsync("  URGENTE!!! Compartilhe antes que apaguem  ", "texto", "nuvem",
                ana.getId(), resultado);

        Analise salva = aguardarGravacao(resultado.getId());
        assertThat(salva.getConteudoOriginal()).isEqualTo("URGENTE!!! Compartilhe antes que apaguem");
        assertThat(salva.getModelo()).isEqualTo("nuvem");
        assertThat(salva.getVeredito()).isEqualTo("FALSO");
        assertThat(salva.getIndicadores()).containsExactly("uso de caixa alta", "apelo emocional");

        mockMvc.perform(comToken(get("/api/analise/historico/{id}", resultado.getId()), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resultado.getId().toString()));
    }

    @Test
    void analiseAnonimaEhSalvaMasNaoApareceEmNenhumHistorico() throws Exception {
        AnaliseResponse resultado = new AnaliseResponse();
        resultado.setId(UUID.randomUUID());
        resultado.setVeredito("VERDADEIRO");
        resultado.setProbabilidadeFake(10);

        historicoService.salvarAnaliseAsync("texto anônimo", "texto", "nuvem", null, resultado);

        assertThat(aguardarGravacao(resultado.getId()).getUsuario()).isNull();
        mockMvc.perform(comToken(get("/api/analise/historico"), tokenAna))
                .andExpect(jsonPath("$.totalItens").value(0));
    }

    // ---------- listagem e isolamento entre usuários ----------

    @Test
    void listarRetornaSomenteAsDoUsuarioMaisRecentesPrimeiroComPaginacao() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        salvar(ana, "mais antiga", "VERDADEIRO", agora.minusHours(2));
        salvar(ana, "do meio", "FALSO", agora.minusHours(1));
        salvar(ana, "mais recente", "FALSO", agora);
        Usuario bruno = usuarioRepository.findByEmail("bruno@exemplo.com").orElseThrow();
        salvar(bruno, "do bruno", "FALSO", agora.plusMinutes(1));

        mockMvc.perform(comToken(get("/api/analise/historico").param("pagina", "0").param("tamanho", "2"), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].conteudoOriginal").value("mais recente"))
                .andExpect(jsonPath("$.itens[1].conteudoOriginal").value("do meio"))
                .andExpect(jsonPath("$.itens[0].indicadores[0]").value("indicador de teste"))
                .andExpect(jsonPath("$.itens[0].notas").isArray())
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        mockMvc.perform(comToken(get("/api/analise/historico").param("pagina", "1").param("tamanho", "2"), tokenAna))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].conteudoOriginal").value("mais antiga"));

        mockMvc.perform(comToken(get("/api/analise/historico"), tokenBruno))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].conteudoOriginal").value("do bruno"));
    }

    @Test
    void outroUsuarioNaoVeNemAlteraAnaliseAlheia() throws Exception {
        Analise daAna = salvar(ana, "privada", "FALSO", LocalDateTime.now());

        mockMvc.perform(comToken(get("/api/analise/historico/{id}", daAna.getId()), tokenBruno))
                .andExpect(status().isNotFound());
        mockMvc.perform(comToken(put("/api/analise/historico/{id}/validacao", daAna.getId()), tokenBruno)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"concorda\": true}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(comToken(post("/api/analise/historico/{id}/notas", daAna.getId()), tokenBruno)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"intrusa\"}"))
                .andExpect(status().isNotFound());

        assertThat(analiseRepository.findById(daAna.getId()).orElseThrow().getAvaliacaoUsuario()).isNull();
    }

    @Test
    void filtraPorVereditoEPorAvaliacao() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        Analise verdadeira = salvar(ana, "verdadeira", "VERDADEIRO", agora);
        salvar(ana, "falsa", "FALSO", agora);

        mockMvc.perform(comToken(get("/api/analise/historico").param("veredito", "falso"), tokenAna))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].veredito").value("FALSO"));

        validar(verdadeira.getId(), "{\"concorda\": false, \"vereditoUsuario\": \"FALSO\"}");

        mockMvc.perform(comToken(get("/api/analise/historico").param("avaliacao", "DISCORDO"), tokenAna))
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].conteudoOriginal").value("verdadeira"));

        mockMvc.perform(comToken(get("/api/analise/historico").param("veredito", "talvez"), tokenAna))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tamanhoDePaginaAcimaDoLimiteELimitadoEIdInvalidoRetorna400() throws Exception {
        mockMvc.perform(comToken(get("/api/analise/historico").param("tamanho", "1000"), tokenAna))
                .andExpect(jsonPath("$.tamanho").value(HistoricoService.TAMANHO_MAXIMO_PAGINA));

        mockMvc.perform(comToken(get("/api/analise/historico/abc"), tokenAna))
                .andExpect(status().isBadRequest());

        mockMvc.perform(comToken(get("/api/analise/historico/{id}", UUID.randomUUID()), tokenAna))
                .andExpect(status().isNotFound());
    }

    // ---------- validação do veredito (Seção IV.H) ----------

    @Test
    void validacaoConcordarDiscordarERemover() throws Exception {
        Analise analise = salvar(ana, "notícia", "FALSO", LocalDateTime.now());

        validar(analise.getId(), "{\"concorda\": true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliacaoUsuario").value("CONCORDO"))
                .andExpect(jsonPath("$.vereditoUsuario").value("FALSO"))
                .andExpect(jsonPath("$.dataAvaliacao").exists());

        validar(analise.getId(), "{\"concorda\": false, \"vereditoUsuario\": \"inconclusivo\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliacaoUsuario").value("DISCORDO"))
                .andExpect(jsonPath("$.vereditoUsuario").value("INCONCLUSIVO"));

        mockMvc.perform(comToken(delete("/api/analise/historico/{id}/validacao", analise.getId()), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avaliacaoUsuario").doesNotExist());

        Analise salva = analiseRepository.findById(analise.getId()).orElseThrow();
        assertThat(salva.getAvaliacaoUsuario()).isNull();
        assertThat(salva.getVereditoUsuario()).isNull();
    }

    @Test
    void validacaoIncoerenteRetorna400() throws Exception {
        Analise analise = salvar(ana, "notícia", "FALSO", LocalDateTime.now());

        validar(analise.getId(), "{\"concorda\": false}")
                .andExpect(status().isBadRequest());
        validar(analise.getId(), "{\"concorda\": false, \"vereditoUsuario\": \"FALSO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O veredito informado é igual ao da IA. Se você concorda, marque \"Concordo\"."));
        validar(analise.getId(), "{}")
                .andExpect(status().isBadRequest());
    }

    // ---------- anotações (Seção IV.H) ----------

    @Test
    void adicionarEExcluirNotas() throws Exception {
        Analise analise = salvar(ana, "notícia", "FALSO", LocalDateTime.now());

        mockMvc.perform(comToken(post("/api/analise/historico/{id}/notas", analise.getId()), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"texto": "  A Agência Lupa já desmentiu.  ", "linkEvidencia": "https://lupa.uol.com.br/checagem"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas.length()").value(1))
                .andExpect(jsonPath("$.notas[0].id").isNumber())
                .andExpect(jsonPath("$.notas[0].texto").value("A Agência Lupa já desmentiu."))
                .andExpect(jsonPath("$.notas[0].linkEvidencia").value("https://lupa.uol.com.br/checagem"));

        String resposta = mockMvc.perform(comToken(post("/api/analise/historico/{id}/notas", analise.getId()), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"Segunda nota, sem link\", \"linkEvidencia\": \"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas.length()").value(2))
                .andExpect(jsonPath("$.notas[1].texto").value("Segunda nota, sem link"))
                .andExpect(jsonPath("$.notas[1].linkEvidencia").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Integer primeiraNotaId = JsonPath.read(resposta, "$.notas[0].id");

        mockMvc.perform(comToken(delete("/api/analise/historico/{id}/notas/{notaId}", analise.getId(), primeiraNotaId), tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notas.length()").value(1))
                .andExpect(jsonPath("$.notas[0].texto").value("Segunda nota, sem link"));

        mockMvc.perform(comToken(delete("/api/analise/historico/{id}/notas/{notaId}", analise.getId(), primeiraNotaId), tokenAna))
                .andExpect(status().isNotFound());
    }

    @Test
    void notaInvalidaRetorna400() throws Exception {
        Analise analise = salvar(ana, "notícia", "FALSO", LocalDateTime.now());

        mockMvc.perform(comToken(post("/api/analise/historico/{id}/notas", analise.getId()), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\": \"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A anotação não pode estar vazia."));

        mockMvc.perform(comToken(post("/api/analise/historico/{id}/notas", analise.getId()), tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\": \"ok\", \"linkEvidencia\": \"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O link de evidência deve começar com http:// ou https://."));
    }

    // ---------- utilitários ----------

    private org.springframework.test.web.servlet.ResultActions validar(UUID id, String json) throws Exception {
        return mockMvc.perform(comToken(put("/api/analise/historico/{id}/validacao", id), tokenAna)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req, String token) {
        return req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private String cadastrar(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Teste", "email": "%s", "senha": "senhaForte123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.token");
    }

    private Analise aguardarGravacao(UUID id) throws InterruptedException {
        // A gravação roda em outra thread: aguarda até ela concluir.
        long limite = System.currentTimeMillis() + 5000;
        while (analiseRepository.findById(id).isEmpty() && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
        }
        return analiseRepository.findById(id).orElseThrow();
    }

    private Analise salvar(Usuario dono, String conteudo, String veredito, LocalDateTime data) {
        Analise analise = new Analise();
        analise.setId(UUID.randomUUID());
        analise.setUsuario(dono);
        analise.setConteudoOriginal(conteudo);
        analise.setOrigem("texto");
        analise.setModelo("nuvem");
        analise.setVeredito(veredito);
        analise.setProbabilidadeFake(50);
        analise.setJustificativa("justificativa de teste");
        analise.setIndicadores(List.of("indicador de teste"));
        analise.setDataAnalise(data);
        return analiseRepository.save(analise);
    }
}
