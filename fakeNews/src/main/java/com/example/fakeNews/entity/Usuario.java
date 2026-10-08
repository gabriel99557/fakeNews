package com.example.fakeNews.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Usuário da plataforma: dono do próprio histórico, validações e anotações (Seção IV.H do TFG). */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    /** Sempre armazenado em minúsculas, sem espaços nas pontas. */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /** Hash BCrypt da senha — a senha em texto puro nunca é armazenada. */
    @Column(nullable = false, length = 100)
    private String senhaHash;

    @Column(nullable = false)
    private LocalDateTime dataCriacao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
}
