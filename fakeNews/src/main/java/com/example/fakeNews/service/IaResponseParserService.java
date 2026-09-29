package com.example.fakeNews.service;

import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.exception.ErroProcessamentoIAException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 5 do pipeline (Geração de Veredito e Indicadores): o modelo de IA é
 * instruído (via prompt) a responder em JSON, mas LLMs às vezes envolvem a
 * resposta em blocos markdown ou texto extra — este serviço normaliza isso
 * e converte para o AnaliseResponse tipado.
 */
@Service
public class IaResponseParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnaliseResponse parsear(String respostaBruta, String origem) {
        try {
            String json = extrairJson(respostaBruta);
            JsonNode node = objectMapper.readTree(json);

            AnaliseResponse resposta = new AnaliseResponse();
            resposta.setVeredito(normalizarVeredito(node.path("veredito").asText("INCONCLUSIVO")));
            resposta.setProbabilidadeFake(clamp(node.path("probabilidadeFake").asInt(50)));
            resposta.setJustificativa(node.path("justificativa").asText(""));
            resposta.setOrigem(origem);

            List<String> indicadores = new ArrayList<>();
            if (node.has("indicadores") && node.get("indicadores").isArray()) {
                node.get("indicadores").forEach(i -> indicadores.add(i.asText()));
            }
            resposta.setIndicadores(indicadores);

            return resposta;
        } catch (Exception e) {
            throw new ErroProcessamentoIAException(
                    "Não foi possível interpretar a resposta do modelo de IA: " + e.getMessage());
        }
    }

    private String extrairJson(String texto) {
        String limpo = texto.trim();
        if (limpo.startsWith("```")) {
            limpo = limpo.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }
        int inicio = limpo.indexOf('{');
        int fim = limpo.lastIndexOf('}');
        if (inicio == -1 || fim == -1 || fim < inicio) {
            throw new IllegalArgumentException("Nenhum JSON válido encontrado na resposta do modelo.");
        }
        return limpo.substring(inicio, fim + 1);
    }

    private String normalizarVeredito(String valor) {
        String v = valor.trim().toUpperCase();
        return switch (v) {
            case "VERDADEIRO", "TRUE", "REAL" -> "VERDADEIRO";
            case "FALSO", "FALSE", "FAKE" -> "FALSO";
            default -> "INCONCLUSIVO";
        };
    }

    private int clamp(int valor) {
        return Math.max(0, Math.min(100, valor));
    }
}
