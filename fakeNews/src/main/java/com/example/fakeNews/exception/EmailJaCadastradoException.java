package com.example.fakeNews.exception;

/** Lançada no cadastro quando o e-mail informado já pertence a outra conta. */
public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String message) {
        super(message);
    }
}
