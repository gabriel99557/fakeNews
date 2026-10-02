package com.example.fakeNews.exception;

/** Lançada quando a requisição é sintaticamente válida, mas viola uma regra de negócio. */
public class RequisicaoInvalidaException extends RuntimeException {
    public RequisicaoInvalidaException(String message) {
        super(message);
    }
}
