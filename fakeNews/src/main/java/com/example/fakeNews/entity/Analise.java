package com.example.fakeNews.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro de uma análise no histórico (RF03 — Histórico de Consultas).
 * Gravado em segundo plano pelo HistoricoService logo após o veredito
 * ser devolvido ao usuário (Fig. 2, "Sincronismo Paralelo Fork/Join").
 */
@Entity
@Table(name = "analises", indexes = {
        @Index(name = "idx_analises_data", columnList = "dataAnalise"),
        @Index(name = "idx_analises_veredito", columnList = "veredito")
})
public class Analise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Texto colado pelo usuário ou a URL informada. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudoOriginal;

    /** "texto" ou "url" */
    @Column(nullable = false, length = 10)
    private String origem;

    /** "nuvem" (Groq) ou "local" (Llama 3 + LoRA via Ollama) */
    @Column(nullable = false, length = 10)
    private String modelo;

    /** VERDADEIRO, FALSO ou INCONCLUSIVO */
    @Column(nullable = false, length = 20)
    private String veredito;

    private Integer probabilidadeFake;

    @Column(columnDefinition = "TEXT")
    private String justificativa;

    /** Armazenado como JSON (JSONB no PostgreSQL), como previsto na Seção II.D do TFG. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> indicadores = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime dataAnalise;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConteudoOriginal() {
        return conteudoOriginal;
    }

    public void setConteudoOriginal(String conteudoOriginal) {
        this.conteudoOriginal = conteudoOriginal;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
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

    public LocalDateTime getDataAnalise() {
        return dataAnalise;
    }

    public void setDataAnalise(LocalDateTime dataAnalise) {
        this.dataAnalise = dataAnalise;
    }
}
