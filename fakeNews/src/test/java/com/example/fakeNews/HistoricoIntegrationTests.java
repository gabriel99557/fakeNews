package com.example.fakeNews;

import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.entity.Analise;
import com.example.fakeNews.repository.AnaliseRepository;
import com.example.fakeNews.service.HistoricoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testes do histórico de análises (RF03) contra um banco H2 em memória. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistoricoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnaliseRepository analiseRepository;

    @Autowired
    private HistoricoService historicoService;

    @BeforeEach
    void limparBanco() {
        analiseRepository.deleteAll();
    }

    @Test
    void salvarAnaliseAsyncPersisteTodosOsCamposEmSegundoPlano() throws Exception {
        AnaliseResponse resultado = new AnaliseResponse();
        resultado.setVeredito("FALSO");
        resultado.setProbabilidadeFake(87);
        resultado.setJustificativa("Linguagem sensacionalista e ausência de fontes.");
        resultado.setIndicadores(List.of("uso de caixa alta", "apelo emocional"));
        resultado.setOrigem("texto");

        historicoService.salvarAnaliseAsync("  URGENTE!!! Compartilhe antes que apaguem  ", "texto", "local", resultado);

        // A gravação roda em outra thread: aguarda até ela concluir.
        long limite = System.currentTimeMillis() + 5000;
        while (analiseRepository.count() == 0 && System.currentTimeMillis() < limite) {
            Thread.sleep(50);
        }

        List<Analise> salvas = analiseRepository.findAll();
        assertThat(salvas).hasSize(1);
        Analise salva = salvas.getFirst();
        assertThat(salva.getConteudoOriginal()).isEqualTo("URGENTE!!! Compartilhe antes que apaguem");
        assertThat(salva.getOrigem()).isEqualTo("texto");
        assertThat(salva.getModelo()).isEqualTo("local");
        assertThat(salva.getVeredito()).isEqualTo("FALSO");
        assertThat(salva.getProbabilidadeFake()).isEqualTo(87);
        assertThat(salva.getIndicadores()).containsExactly("uso de caixa alta", "apelo emocional");
        assertThat(salva.getDataAnalise()).isNotNull();
    }

    @Test
    void listarRetornaMaisRecentesPrimeiroComPaginacao() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        salvar("mais antiga", "VERDADEIRO", agora.minusHours(2));
        salvar("do meio", "FALSO", agora.minusHours(1));
        salvar("mais recente", "FALSO", agora);

        mockMvc.perform(get("/api/analise/historico").param("pagina", "0").param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].conteudoOriginal").value("mais recente"))
                .andExpect(jsonPath("$.itens[1].conteudoOriginal").value("do meio"))
                .andExpect(jsonPath("$.itens[0].indicadores[0]").value("indicador de teste"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(2))
                .andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));

        mockMvc.perform(get("/api/analise/historico").param("pagina", "1").param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].conteudoOriginal").value("mais antiga"));
    }

    @Test
    void listarFiltraPorVereditoIgnorandoMaiusculas() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        salvar("verdadeira", "VERDADEIRO", agora);
        salvar("falsa", "FALSO", agora);

        mockMvc.perform(get("/api/analise/historico").param("veredito", "falso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].veredito").value("FALSO"));
    }

    @Test
    void tamanhoDePaginaAcimaDoLimiteELimitado() throws Exception {
        mockMvc.perform(get("/api/analise/historico").param("tamanho", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tamanho").value(HistoricoService.TAMANHO_MAXIMO_PAGINA));
    }

    @Test
    void buscarPorIdRetornaAnaliseOu404() throws Exception {
        Analise salva = salvar("detalhe", "INCONCLUSIVO", LocalDateTime.now());

        mockMvc.perform(get("/api/analise/historico/{id}", salva.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salva.getId()))
                .andExpect(jsonPath("$.veredito").value("INCONCLUSIVO"));

        mockMvc.perform(get("/api/analise/historico/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(get("/api/analise/historico/abc"))
                .andExpect(status().isBadRequest());
    }

    private Analise salvar(String conteudo, String veredito, LocalDateTime data) {
        Analise analise = new Analise();
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
