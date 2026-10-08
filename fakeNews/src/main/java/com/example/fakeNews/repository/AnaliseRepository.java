package com.example.fakeNews.repository;

import com.example.fakeNews.entity.Analise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso a dados do histórico de análises (RF03). */
public interface AnaliseRepository extends JpaRepository<Analise, Long> {

    Page<Analise> findByVeredito(String veredito, Pageable pageable);
}
