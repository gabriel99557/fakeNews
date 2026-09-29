package com.example.fakeNews.exception;

/** Lançada quando a chamada ao motor de IA falha ou retorna uma resposta não interpretável. */
public class ErroProcessamentoIAException extends RuntimeException {
    public ErroProcessamentoIAException(String message) {
        super(message);
    }
}
