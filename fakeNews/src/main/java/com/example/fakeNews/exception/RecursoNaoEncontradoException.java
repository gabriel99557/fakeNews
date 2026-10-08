package com.example.fakeNews.exception;

/** Lançada quando um registro solicitado (ex.: uma análise do histórico) não existe. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
