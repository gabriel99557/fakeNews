package com.example.fakeNews.service;

import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.exception.ErroProcessamentoIAException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Orquestrador principal do pipeline de análise (Fig. 1 do TFG 1):
 * validação de entrada -> (scraping se URL) -> higienização ->
 * inferência de IA -> geração de veredito estruturado -> persistência assíncrona.
 */
@Service
public class AnaliseTextoService {

    private static final String PROMPT_SISTEMA = """
            Você é um especialista em Processamento de Linguagem Natural e checagem de fatos (fact-checking).
            Analise o texto fornecido e responda EXCLUSIVAMENTE em formato JSON, sem nenhum texto adicional,
            comentário ou marcação markdown (sem ```), seguindo estritamente este esquema:
            {
              "veredito": "VERDADEIRO" | "FALSO" | "INCONCLUSIVO",
              "probabilidadeFake": <inteiro de 0 a 100, sendo 100 = altíssima probabilidade de ser fake news>,
              "justificativa": "<explicação curta e didática, em português, de até 3 frases>",
              "indicadores": ["<indicador linguístico 1>", "<indicador linguístico 2>"]
            }
            """;

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private static final String OLLAMA_MODEL = "meu-llama-tcc";

    private final ChatClient chatClient;
    private final WebScrapingService webScrapingService;
    private final ConteudoValidatorService validatorService;
    private final IaResponseParserService parserService;
    private final HistoricoService historicoService;

    public AnaliseTextoService(ChatClient.Builder builder,
                                WebScrapingService webScrapingService,
                                ConteudoValidatorService validatorService,
                                IaResponseParserService parserService,
                                HistoricoService historicoService) {
        this.chatClient = builder.build();
        this.webScrapingService = webScrapingService;
        this.validatorService = validatorService;
        this.parserService = parserService;
        this.historicoService = historicoService;
    }

    /** Pipeline usando o modelo em nuvem (Groq/OpenAI-compatible via Spring AI). */
    public AnaliseResponse analisar(String conteudo) {
        return executarPipeline(conteudo, "nuvem", this::chamarIaNuvem);
    }

    /** Mesmo pipeline, usando o modelo local fine-tuned (Llama 3 + LoRA via Ollama). */
    public AnaliseResponse analisarLocal(String conteudo) {
        return executarPipeline(conteudo, "local", this::chamarIaLocal);
    }

    private AnaliseResponse executarPipeline(String conteudo, String modelo, Function<String, String> chamadaIa) {
        String origem;
        String textoParaAnalise;

        if (validatorService.isUrl(conteudo)) {
            validatorService.validarUrl(conteudo);
            textoParaAnalise = webScrapingService.extrairTextoDeUrl(conteudo.trim());
            origem = "url";
        } else {
            validatorService.validarTexto(conteudo);
            textoParaAnalise = conteudo.trim();
            origem = "texto";
        }

        String textoHigienizado = higienizar(textoParaAnalise);
        String respostaBruta = chamadaIa.apply(textoHigienizado);
        AnaliseResponse resultado = parserService.parsear(respostaBruta, origem);

        //  dispara a persistência em background
        // sem bloquear a resposta que já será devolvida ao usuário.
        historicoService.salvarAnaliseAsync(conteudo, origem, modelo, resultado);

        return resultado;
    }

    private String higienizar(String texto) {
        return texto.replaceAll("\\s+", " ").trim();
    }

    private String chamarIaNuvem(String texto) {
        try {
            return chatClient.prompt()
                    .system(PROMPT_SISTEMA)
                    .user(texto)
                    .call()
                    .content();
        } catch (Exception e) {
            throw new ErroProcessamentoIAException("Erro ao consultar o modelo de IA em nuvem: " + e.getMessage());
        }
    }

    private String chamarIaLocal(String texto) {
        RestTemplate restTemplate = new RestTemplate();

        Map<String, Object> request = new HashMap<>();
        request.put("model", OLLAMA_MODEL);
        request.put("prompt", PROMPT_SISTEMA + "\n\nTexto para análise:\n" + texto);
        request.put("stream", false);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(OLLAMA_API_URL, request, Map.class);
            if (response == null || response.get("response") == null) {
                throw new ErroProcessamentoIAException("Resposta vazia do modelo local.");
            }
            return (String) response.get("response");
        } catch (ErroProcessamentoIAException e) {
            throw e;
        } catch (Exception e) {
            throw new ErroProcessamentoIAException("Erro ao conectar com a IA local (Ollama): " + e.getMessage());
        }
    }
}
