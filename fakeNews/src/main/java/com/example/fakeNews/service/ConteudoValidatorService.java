package com.example.fakeNews.service;

import com.example.fakeNews.exception.TextoInvalidoException;
import com.example.fakeNews.exception.UrlInvalidaException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Nó de decisão e bifurcação lógica descrito na Fig. 2 do TFG 1:
 * decide se a entrada do usuário é uma URL ou um texto bruto, e valida
 * cada caso antes de seguir no pipeline.
 */
@Service
public class ConteudoValidatorService {

    private static final int TAMANHO_MINIMO_TEXTO = 30;
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?)://.+", Pattern.CASE_INSENSITIVE);

    public boolean isUrl(String conteudo) {
        return conteudo != null && URL_PATTERN.matcher(conteudo.trim()).matches();
    }

    public void validarUrl(String url) {
        try {
            URI uri = new URI(url.trim());
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new UrlInvalidaException("A URL informada é inválida.");
            }
        } catch (URISyntaxException e) {
            throw new UrlInvalidaException("A URL informada é inválida.");
        }
    }

    public void validarTexto(String texto) {
        if (texto == null || texto.trim().length() < TAMANHO_MINIMO_TEXTO) {
            throw new TextoInvalidoException(
                    "O texto informado é muito curto para uma análise confiável (mínimo de "
                            + TAMANHO_MINIMO_TEXTO + " caracteres).");
        }
    }
}
