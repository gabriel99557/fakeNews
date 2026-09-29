package com.example.fakeNews.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnaliseResponse {

    /** VERDADEIRO, FALSO ou INCONCLUSIVO */
    private String veredito;

    /** 0 a 100 — quanto maior, maior a probabilidade de ser desinformação */
    private Integer probabilidadeFake;

    private String justificativa;

    private List<String> indicadores;

    /** "texto" ou "url" — de onde o conteúdo analisado se originou */
    private String origem;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataAnalise;

    public AnaliseResponse() {
        this.dataAnalise = LocalDateTime.now();
    }

    public String getVeredito() {
        return veredito;
    }

    public void setVeredito(String veredito) {
        this.veredito = veredito;
    }

    public Integer getProbabilidadeFake() {
        return probabilidadeFake;
    }

    public void setProbabilidadeFake(Integer probabilidadeFake) {
        this.probabilidadeFake = probabilidadeFake;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public List<String> getIndicadores() {
        return indicadores;
    }

    public void setIndicadores(List<String> indicadores) {
        this.indicadores = indicadores;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public LocalDateTime getDataAnalise() {
        return dataAnalise;
    }

    public void setDataAnalise(LocalDateTime dataAnalise) {
        this.dataAnalise = dataAnalise;
    }
}
