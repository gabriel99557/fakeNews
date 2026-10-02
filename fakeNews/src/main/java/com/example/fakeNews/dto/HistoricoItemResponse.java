package com.example.fakeNews.dto;

import com.example.fakeNews.entity.Analise;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Um registro do histórico de análises, com a validação e as anotações do usuário (RF03 + Seção IV.H). */
public record HistoricoItemResponse(
        UUID id,
        String conteudoOriginal,
        String origem,
        String modelo,
        String veredito,
        Integer probabilidadeFake,
        String justificativa,
        List<String> indicadores,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataAnalise,
        /** CONCORDO, DISCORDO ou null (ainda não avaliado) */
        String avaliacaoUsuario,
        String vereditoUsuario,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataAvaliacao,
        List<NotaResponse> notas
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
                analise.getDataAnalise(),
                analise.getAvaliacaoUsuario() != null ? analise.getAvaliacaoUsuario().name() : null,
                analise.getVereditoUsuario(),
                analise.getDataAvaliacao(),
                analise.getNotas().stream().map(NotaResponse::de).toList()
        );
    }
}
