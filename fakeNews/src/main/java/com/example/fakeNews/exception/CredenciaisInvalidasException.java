package com.example.fakeNews.exception;

/** Lançada no login quando e-mail ou senha não conferem (mensagem genérica de propósito). */
public class CredenciaisInvalidasException extends RuntimeException {
    public CredenciaisInvalidasException(String message) {
        super(message);
    }
}
