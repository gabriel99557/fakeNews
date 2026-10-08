package com.example.fakeNews.repository;

import com.example.fakeNews.entity.Analise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Acesso a dados do histórico de análises (RF03). Toda consulta é restrita ao dono. */
public interface AnaliseRepository extends JpaRepository<Analise, UUID> {

    @Query("""
            select a from Analise a
            where a.usuario.id = :usuarioId
              and (:veredito is null or a.veredito = :veredito)
              and (:avaliacao is null or a.avaliacaoUsuario = :avaliacao)
            """)
    Page<Analise> buscarDoUsuario(@Param("usuarioId") Long usuarioId,
                                  @Param("veredito") String veredito,
                                  @Param("avaliacao") Analise.Avaliacao avaliacao,
                                  Pageable pageable);

    Optional<Analise> findByIdAndUsuarioId(UUID id, Long usuarioId);
}
