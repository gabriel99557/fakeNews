package com.example.fakeNews.controller;

import com.example.fakeNews.config.OpenApiConfig;
import com.example.fakeNews.dto.HistoricoItemResponse;
import com.example.fakeNews.dto.NotaRequest;
import com.example.fakeNews.dto.PaginaResponse;
import com.example.fakeNews.dto.ValidacaoRequest;
import com.example.fakeNews.service.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Histórico pessoal (RF03) com validação e anotações do usuário (Seção IV.H). Exige login. */
@RestController
@RequestMapping("/api/analise/historico")
@Tag(name = "Histórico de Análises", description = "Histórico pessoal, validação do veredito e anotações (requer login)")
@SecurityRequirement(name = OpenApiConfig.BEARER)
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @Operation(
        summary = "Lista o histórico do usuário logado (paginado)",
        description = "Retorna as análises mais recentes primeiro. Pode ser filtrado por veredito da IA "
                + "e pela avaliação do usuário (ex.: só as que ele contestou)."
    )
    @GetMapping
    public PaginaResponse<HistoricoItemResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "VERDADEIRO, FALSO ou INCONCLUSIVO (opcional)")
            @RequestParam(required = false) String veredito,
            @Parameter(description = "CONCORDO ou DISCORDO (opcional)")
            @RequestParam(required = false) String avaliacao,
            @Parameter(description = "Número da página, começando em 0")
            @RequestParam(defaultValue = "0") int pagina,
            @Parameter(description = "Itens por página (máximo " + HistoricoService.TAMANHO_MAXIMO_PAGINA + ")")
            @RequestParam(defaultValue = "10") int tamanho) {
        return historicoService.listar(usuarioId(jwt), veredito, avaliacao, pagina, tamanho);
    }

    @Operation(
        summary = "Detalha uma análise do histórico",
        responses = {
            @ApiResponse(responseCode = "200", description = "Análise encontrada"),
            @ApiResponse(responseCode = "404", description = "Análise não encontrada no histórico do usuário")
        }
    )
    @GetMapping("/{id}")
    public HistoricoItemResponse buscarPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return historicoService.buscarPorId(usuarioId(jwt), id);
    }

    @Operation(summary = "Registra se o usuário concorda com o veredito da IA (e, se não, qual seria o correto)")
    @PutMapping("/{id}/validacao")
    public HistoricoItemResponse validar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                         @Valid @RequestBody ValidacaoRequest request) {
        return historicoService.validar(usuarioId(jwt), id, request);
    }

    @Operation(summary = "Remove a validação do usuário")
    @DeleteMapping("/{id}/validacao")
    public HistoricoItemResponse removerValidacao(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return historicoService.removerValidacao(usuarioId(jwt), id);
    }

    @Operation(summary = "Adiciona uma anotação pessoal (apontamento, contraponto ou evidência)")
    @PostMapping("/{id}/notas")
    public HistoricoItemResponse adicionarNota(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                               @Valid @RequestBody NotaRequest request) {
        return historicoService.adicionarNota(usuarioId(jwt), id, request);
    }

    @Operation(summary = "Exclui uma anotação pessoal")
    @DeleteMapping("/{id}/notas/{notaId}")
    public HistoricoItemResponse removerNota(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                             @PathVariable Long notaId) {
        return historicoService.removerNota(usuarioId(jwt), id, notaId);
    }

    private static Long usuarioId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
