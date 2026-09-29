package com.example.fakeNews.exception;

/** Lançada quando o texto bruto informado é curto/insuficiente para análise. */
public class TextoInvalidoException extends RuntimeException {
    public TextoInvalidoException(String message) {
        super(message);
    }
}
