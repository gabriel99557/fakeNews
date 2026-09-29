package com.example.fakeNews.exception;

/** Lançada quando o web scraping falha ou não encontra conteúdo textual suficiente. */
public class ErroExtracaoConteudoException extends RuntimeException {
    public ErroExtracaoConteudoException(String message) {
        super(message);
    }
}
