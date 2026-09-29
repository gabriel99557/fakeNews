package com.example.fakeNews.dto;

import jakarta.validation.constraints.NotBlank;

public class AnaliseRequest {

    @NotBlank(message = "O campo 'conteudo' não pode estar vazio.")
    private String conteudo;

    public AnaliseRequest() {
    }

    public AnaliseRequest(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
}
