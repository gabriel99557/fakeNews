package com.example.fakeNews.service;

import com.example.fakeNews.dto.AnaliseResponse;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Persistência do histórico de análises (RF03), disparada em segundo plano
 * (RNF02 / Fig. 2 "Sincronismo Paralelo Fork-Join") para não atrasar a
 * resposta síncrona já devolvida ao usuário.
 *
 * A integração real com o banco (PostgreSQL/JPA) ainda não está ativa —
 * ver entity/AnaliseEntity.java e repository/AnaliseRepository.java para
 * o passo a passo de ativação. Por enquanto, este serviço apenas loga a
 * análise que seria persistida, mantendo o pipeline assíncrono já
 * funcional e pronto para receber a chamada real ao repository.
 */
@Service
public class HistoricoService {

    // Descomentar quando o banco estiver configurado:
    // private final AnaliseRepository analiseRepository;
    //
    // public HistoricoService(AnaliseRepository analiseRepository) {
    //     this.analiseRepository = analiseRepository;
    // }

    @Async("taskExecutor")
    public void salvarAnaliseAsync(String conteudoOriginal, String origem, AnaliseResponse resultado) {
        // TODO: substituir o log abaixo pela persistência real (analiseRepository.save(...))
        // assim que entity/AnaliseEntity.java e repository/AnaliseRepository.java
        // forem descomentados e o datasource configurado.
        System.out.printf(
                "[HISTORICO] %s | origem=%s | veredito=%s | probabilidadeFake=%d%%%n",
                LocalDateTime.now(), origem, resultado.getVeredito(), resultado.getProbabilidadeFake()
        );

        /*
        AnaliseEntity.Analise entidade = new AnaliseEntity.Analise();
        entidade.setConteudoOriginal(conteudoOriginal);
        entidade.setOrigem(origem);
        entidade.setVeredito(resultado.getVeredito());
        entidade.setProbabilidadeFake(resultado.getProbabilidadeFake());
        entidade.setJustificativa(resultado.getJustificativa());
        entidade.setDataAnalise(resultado.getDataAnalise());
        analiseRepository.save(entidade);
        */
    }
}
