package com.example.fakeNews.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Validação pessoal do veredito da IA (Seção IV.H do TFG).
 * Ao discordar, o usuário informa qual seria o veredito correto na opinião dele.
 */
public record ValidacaoRequest(
        @NotNull(message = "Informe se você concorda ou não com o veredito.")
        Boolean concorda,

        /** VERDADEIRO, FALSO ou INCONCLUSIVO — obrigatório quando concorda = false. */
        String vereditoUsuario
) {
}
