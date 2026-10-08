package com.example.fakeNews.controller;

import com.example.fakeNews.dto.HistoricoItemResponse;
import com.example.fakeNews.dto.PaginaResponse;
import com.example.fakeNews.service.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analise/historico")
@CrossOrigin(origins = "*")
@Tag(name = "Histórico de Análises", description = "Consulta das análises anteriores (RF03)")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @Operation(
        summary = "Lista o histórico de análises (paginado)",
        description = "Retorna as análises mais recentes primeiro. Pode ser filtrado por veredito."
    )
    @GetMapping
    public PaginaResponse<HistoricoItemResponse> listar(
            @Parameter(description = "VERDADEIRO, FALSO ou INCONCLUSIVO (opcional)")
            @RequestParam(required = false) String veredito,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Itens por página (máximo " + HistoricoService.TAMANHO_MAXIMO_PAGINA + ")")
            @RequestParam(defaultValue = "10") int tamanho) {
        return historicoService.listar(veredito, pagina, tamanho);
    }

    @Operation(
        summary = "Detalha uma análise do histórico",
        responses = {
            @ApiResponse(responseCode = "200", description = "Análise encontrada"),
            @ApiResponse(responseCode = "404", description = "Análise não encontrada")
        }
    )
    @GetMapping("/{id}")
    public HistoricoItemResponse buscarPorId(@PathVariable Long id) {
        return historicoService.buscarPorId(id);
    }
}
