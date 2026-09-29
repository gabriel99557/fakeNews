package com.example.fakeNews.service;

import com.example.fakeNews.exception.ErroExtracaoConteudoException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/**
 * Módulo de raspagem de conteúdo (RF01 / etapa 2 do pipeline da Fig. 1):
 * quando o usuário fornece uma URL em vez de texto bruto, este serviço
 * extrai o corpo textual da notícia, descartando tags HTML irrelevantes.
 */
@Service
public class WebScrapingService {

    private static final int TIMEOUT_MS = 10_000;
    private static final int TAMANHO_MINIMO_TEXTO = 100;

    public String extrairTextoDeUrl(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; FakeNewsChecker/1.0)")
                    .timeout(TIMEOUT_MS)
                    .get();

            // Prioriza parágrafos dentro de <article>/<main>, com fallback para todos os <p>
            Elements paragrafos = doc.select("article p, main p");
            if (paragrafos.isEmpty()) {
                paragrafos = doc.select("p");
            }

            StringBuilder corpoTexto = new StringBuilder();
            for (var paragrafo : paragrafos) {
                corpoTexto.append(paragrafo.text()).append(" ");
            }

            String titulo = doc.title();
            String corpo = corpoTexto.toString().trim();

            if (corpo.length() < TAMANHO_MINIMO_TEXTO) {
                throw new ErroExtracaoConteudoException(
                        "Não foi possível extrair conteúdo textual suficiente da URL informada. "
                                + "O site pode bloquear scraping ou não conter texto de notícia identificável.");
            }

            return (titulo != null && !titulo.isBlank() ? titulo + ". " : "") + corpo;

        } catch (ErroExtracaoConteudoException e) {
            throw e;
        } catch (Exception e) {
            throw new ErroExtracaoConteudoException(
                    "Falha ao acessar ou processar a URL informada: " + e.getMessage());
        }
    }
}
