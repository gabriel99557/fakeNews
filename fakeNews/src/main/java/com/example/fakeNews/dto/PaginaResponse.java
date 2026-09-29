package com.example.fakeNews.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação estável para a API. Evita serializar o Page do
 * Spring Data diretamente, cujo formato JSON não é garantido entre versões.
 */
public record PaginaResponse<T>(
        List<T> itens,
        int pagina,
        int tamanho,
        long totalItens,
        int totalPaginas
) {

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> conversor) {
        return new PaginaResponse<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
