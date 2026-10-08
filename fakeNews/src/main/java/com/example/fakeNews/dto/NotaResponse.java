package com.example.fakeNews.dto;

import com.example.fakeNews.entity.Nota;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record NotaResponse(
        Long id,
        String texto,
        String linkEvidencia,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime dataCriacao
) {

    public static NotaResponse de(Nota nota) {
        return new NotaResponse(nota.getId(), nota.getTexto(), nota.getLinkEvidencia(), nota.getDataCriacao());
    }
}
