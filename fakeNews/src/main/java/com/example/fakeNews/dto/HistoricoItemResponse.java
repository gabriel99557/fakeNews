package com.example.fakeNews.dto;

import com.example.fakeNews.entity.Analise;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/** Um registro do histórico de análises, como exposto pela API (RF03). */
public record HistoricoItemResponse(
        Long id,
        String conteudoOriginal,
        String origem,
        String modelo,
        String veredito,
        Integer probabilidadeFake,
        String justificativa,
        List<String> indicadores,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataAnalise
) {

    public static HistoricoItemResponse de(Analise analise) {
        return new HistoricoItemResponse(
                analise.getId(),
                analise.getConteudoOriginal(),
                analise.getOrigem(),
                analise.getModelo(),
                analise.getVeredito(),
                analise.getProbabilidadeFake(),
                analise.getJustificativa(),
                analise.getIndicadores(),
                analise.getDataAnalise()
        );
    }
}
