package com.example.fakeNews.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Registro de uma análise no histórico (RF03 — Histórico de Consultas).
 * Gravado em segundo plano pelo HistoricoService logo após o veredito
 * ser devolvido ao usuário (Fig. 2, "Sincronismo Paralelo Fork/Join").
 *
 * O id é um UUID gerado pelo pipeline ANTES da resposta, para que o
 * usuário já possa validar/anotar o resultado mesmo com a gravação
 * acontecendo de forma assíncrona.
 */
@Entity
@Table(name = "analises", indexes = {
        @Index(name = "idx_analises_usuario_data", columnList = "usuario_id, dataAnalise"),
        @Index(name = "idx_analises_veredito", columnList = "veredito")
})
public class Analise {

    @Id
    private UUID id;

    /** Dono da análise; nulo quando a análise foi feita sem login (fica só como log de auditoria). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

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

    // --- Validação pessoal do resultado (Seção IV.H) ---

    /** CONCORDO ou DISCORDO; nulo enquanto o usuário não avaliou. */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Avaliacao avaliacaoUsuario;

    /** Veredito na opinião do usuário (igual ao da IA quando ele concorda). */
    @Column(length = 20)
    private String vereditoUsuario;

    private LocalDateTime dataAvaliacao;

    @OneToMany(mappedBy = "analise", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dataCriacao ASC, id ASC")
    @BatchSize(size = 50)
    private List<Nota> notas = new ArrayList<>();

    public enum Avaliacao {
        CONCORDO, DISCORDO
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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

    public Avaliacao getAvaliacaoUsuario() {
        return avaliacaoUsuario;
    }

    public void setAvaliacaoUsuario(Avaliacao avaliacaoUsuario) {
        this.avaliacaoUsuario = avaliacaoUsuario;
    }

    public String getVereditoUsuario() {
        return vereditoUsuario;
    }

    public void setVereditoUsuario(String vereditoUsuario) {
        this.vereditoUsuario = vereditoUsuario;
    }

    public LocalDateTime getDataAvaliacao() {
        return dataAvaliacao;
    }

    public void setDataAvaliacao(LocalDateTime dataAvaliacao) {
        this.dataAvaliacao = dataAvaliacao;
    }

    public List<Nota> getNotas() {
        return notas;
    }

    public void setNotas(List<Nota> notas) {
        this.notas = notas;
    }
}
