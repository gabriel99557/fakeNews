package com.example.fakeNews.controller;

import com.example.fakeNews.dto.AnaliseRequest;
import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.service.AnaliseTextoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

//http://localhost:8080/swagger-ui/index.html#/An%C3%A1lise%20de%20Fake%20News/analisarConteudoLocal
@RestController
@RequestMapping("/api/analise")
@CrossOrigin(origins = "*")
@Tag(name = "Análise de Fake News", description = "Endpoints para verificação de desinformação com IA")
public class AnaliseTextoController {

    private final AnaliseTextoService analiseTextoService;

    public AnaliseTextoController(AnaliseTextoService analiseTextoService) {
        this.analiseTextoService = analiseTextoService;
    }

    @Operation(
        summary = "Analisa um texto ou URL de notícia (modelo em nuvem)",
        description = "Aceita texto bruto OU uma URL de notícia no mesmo campo. Detecta automaticamente " +
                "o tipo de entrada, extrai o conteúdo via web scraping quando necessário, e retorna um " +
                "veredito estruturado (veredito, probabilidade, justificativa e indicadores linguísticos).",
        responses = {
            @ApiResponse(responseCode = "200", description = "Análise concluída com sucesso"),
            @ApiResponse(responseCode = "400", description = "Entrada inválida (texto curto ou URL malformada)"),
            @ApiResponse(responseCode = "422", description = "Não foi possível extrair conteúdo da URL"),
            @ApiResponse(responseCode = "502", description = "Erro ao processar a requisição na IA")
        }
    )
    @PostMapping("/texto")
    public AnaliseResponse analisarConteudo(@Valid @RequestBody AnaliseRequest request) {
        return analiseTextoService.analisar(request.getConteudo());
    }

    @Operation(
        summary = "Analisa um texto ou URL de notícia (modelo local fine-tuned)",
        description = "Mesmo pipeline do endpoint /texto, porém consultando o modelo Llama 3 " +
                "otimizado com LoRA, servido localmente via Ollama."
    )
    @PostMapping("/texto/local")
    public AnaliseResponse analisarConteudoLocal(@Valid @RequestBody AnaliseRequest request) {
        return analiseTextoService.analisarLocal(request.getConteudo());
    }
}
