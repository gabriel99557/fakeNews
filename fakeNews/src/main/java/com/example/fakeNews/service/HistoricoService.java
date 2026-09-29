package com.example.fakeNews.service;

import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.dto.HistoricoItemResponse;
import com.example.fakeNews.dto.PaginaResponse;
import com.example.fakeNews.entity.Analise;
import com.example.fakeNews.exception.RecursoNaoEncontradoException;
import com.example.fakeNews.repository.AnaliseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * Histórico de análises (RF03). A gravação é disparada em segundo plano
 * (RNF02 / Fig. 2 "Sincronismo Paralelo Fork-Join") para não atrasar a
 * resposta síncrona já devolvida ao usuário; a leitura é paginada, como
 * descrito na Camada de Persistência do TFG (Seção IV.D.4).
 */
@Service
public class HistoricoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 50;

    private static final Logger log = LoggerFactory.getLogger(HistoricoService.class);

    private final AnaliseRepository analiseRepository;

    public HistoricoService(AnaliseRepository analiseRepository) {
        this.analiseRepository = analiseRepository;
    }

    @Async("taskExecutor")
    public void salvarAnaliseAsync(String conteudoOriginal, String origem, String modelo, AnaliseResponse resultado) {
        try {
            Analise analise = new Analise();
            analise.setConteudoOriginal(conteudoOriginal.trim());
            analise.setOrigem(origem);
            analise.setModelo(modelo);
            analise.setVeredito(resultado.getVeredito());
            analise.setProbabilidadeFake(resultado.getProbabilidadeFake());
            analise.setJustificativa(resultado.getJustificativa());
            analise.setIndicadores(resultado.getIndicadores() != null
                    ? new ArrayList<>(resultado.getIndicadores())
                    : new ArrayList<>());
            analise.setDataAnalise(resultado.getDataAnalise());

            Analise salva = analiseRepository.save(analise);
            log.info("[HISTORICO] análise #{} salva | origem={} | modelo={} | veredito={} | probabilidadeFake={}%",
                    salva.getId(), origem, modelo, salva.getVeredito(), salva.getProbabilidadeFake());
        } catch (Exception e) {
            // A resposta já foi entregue ao usuário: uma falha aqui não pode
            // derrubar a requisição, apenas precisa ficar registrada.
            log.error("[HISTORICO] falha ao salvar análise no banco de dados", e);
        }
    }

    public PaginaResponse<HistoricoItemResponse> listar(String veredito, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "dataAnalise").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Analise> page = (veredito == null || veredito.isBlank())
                ? analiseRepository.findAll(pageRequest)
                : analiseRepository.findByVeredito(veredito.trim().toUpperCase(), pageRequest);

        return PaginaResponse.de(page, HistoricoItemResponse::de);
    }

    public HistoricoItemResponse buscarPorId(Long id) {
        return analiseRepository.findById(id)
                .map(HistoricoItemResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Análise #" + id + " não encontrada no histórico."));
    }
}
