package com.example.fakeNews.entity;

/**
 * Entidade JPA que representa um registro de análise no histórico
 * (RF03 — Histórico de Consultas).
 *
 * AINDA NÃO ATIVA. Para habilitar, quando for implementar o PostgreSQL:
 *   1. No pom.xml, descomente o bloco de dependências de JPA + PostgreSQL
 *      (procure o comentário "DEPENDÊNCIAS DE BANCO DE DADOS").
 *   2. Descomente o corpo desta classe abaixo.
 *   3. Configure spring.datasource.* no application.properties.
 *   4. Descomente o AnaliseRepository (repository/AnaliseRepository.java).
 *   5. Descomente a injeção do repository e o bloco de persistência em
 *      service/HistoricoService.java.
 */
public class AnaliseEntity {

    /*
    @jakarta.persistence.Entity
    @jakarta.persistence.Table(name = "analises")
    public static class Analise {

        @jakarta.persistence.Id
        @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
        private Long id;

        @jakarta.persistence.Column(columnDefinition = "TEXT", nullable = false)
        private String conteudoOriginal;

        @jakarta.persistence.Column(nullable = false)
        private String origem; // "texto" ou "url"

        @jakarta.persistence.Column(nullable = false)
        private String veredito; // VERDADEIRO, FALSO, INCONCLUSIVO

        private Integer probabilidadeFake;

        @jakarta.persistence.Column(columnDefinition = "TEXT")
        private String justificativa;

        @jakarta.persistence.Column(columnDefinition = "TEXT")
        private String indicadores; // serializado como JSON (ou usar @ElementCollection)

        @jakarta.persistence.Column(nullable = false)
        private java.time.LocalDateTime dataAnalise;

        // getters e setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getConteudoOriginal() { return conteudoOriginal; }
        public void setConteudoOriginal(String conteudoOriginal) { this.conteudoOriginal = conteudoOriginal; }

        public String getOrigem() { return origem; }
        public void setOrigem(String origem) { this.origem = origem; }

        public String getVeredito() { return veredito; }
        public void setVeredito(String veredito) { this.veredito = veredito; }

        public Integer getProbabilidadeFake() { return probabilidadeFake; }
        public void setProbabilidadeFake(Integer probabilidadeFake) { this.probabilidadeFake = probabilidadeFake; }

        public String getJustificativa() { return justificativa; }
        public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

        public String getIndicadores() { return indicadores; }
        public void setIndicadores(String indicadores) { this.indicadores = indicadores; }

        public java.time.LocalDateTime getDataAnalise() { return dataAnalise; }
        public void setDataAnalise(java.time.LocalDateTime dataAnalise) { this.dataAnalise = dataAnalise; }
    }
    */
}
