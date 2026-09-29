package com.example.fakeNews.exception;

import com.example.fakeNews.dto.ErroResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Centraliza o tratamento de erros da API, garantindo que toda falha
 * (validação, scraping, IA ou erro inesperado) retorne um JSON padronizado
 * em vez de vazar stack traces ou mensagens inconsistentes ao cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Requisição inválida.");
        return construir(HttpStatus.BAD_REQUEST, mensagem);
    }

    @ExceptionHandler(TextoInvalidoException.class)
    public ResponseEntity<ErroResponse> handleTextoInvalido(TextoInvalidoException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UrlInvalidaException.class)
    public ResponseEntity<ErroResponse> handleUrlInvalida(UrlInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ErroExtracaoConteudoException.class)
    public ResponseEntity<ErroResponse> handleErroExtracao(ErroExtracaoConteudoException ex) {
        return construir(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ErroProcessamentoIAException.class)
    public ResponseEntity<ErroResponse> handleErroIA(ErroProcessamentoIAException ex) {
        return construir(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(Exception ex) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado: " + ex.getMessage());
    }

    private ResponseEntity<ErroResponse> construir(HttpStatus status, String mensagem) {
        ErroResponse erro = new ErroResponse(status.value(), mensagem, LocalDateTime.now());
        return ResponseEntity.status(status).body(erro);
    }
}
