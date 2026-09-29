package com.example.fakeNews.exception;

/** Lançada quando a URL informada não é sintaticamente válida. */
public class UrlInvalidaException extends RuntimeException {
    public UrlInvalidaException(String message) {
        super(message);
    }
}
