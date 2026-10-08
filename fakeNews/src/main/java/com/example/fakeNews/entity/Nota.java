package com.example.fakeNews.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Anotação pessoal do usuário sobre uma análise (Seção IV.H do TFG):
 * apontamentos, contrapontos ou evidências guardadas para consultas futuras.
 */
@Entity
@Table(name = "notas")
public class Nota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analise_id", nullable = false)
    private Analise analise;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String texto;

    /** Link opcional para uma evidência (matéria, checagem, documento oficial). */
    @Column(length = 2048)
    private String linkEvidencia;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Analise getAnalise() {
        return analise;
    }

    public void setAnalise(Analise analise) {
        this.analise = analise;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getLinkEvidencia() {
        return linkEvidencia;
    }

    public void setLinkEvidencia(String linkEvidencia) {
        this.linkEvidencia = linkEvidencia;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
