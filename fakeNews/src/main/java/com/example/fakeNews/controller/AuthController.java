package com.example.fakeNews.controller;

import com.example.fakeNews.config.OpenApiConfig;
import com.example.fakeNews.dto.auth.AuthResponse;
import com.example.fakeNews.dto.auth.CadastroRequest;
import com.example.fakeNews.dto.auth.LoginRequest;
import com.example.fakeNews.dto.auth.UsuarioResponse;
import com.example.fakeNews.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Cadastro, login e dados do usuário logado")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Cria uma conta e já devolve o token de acesso",
        responses = {
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
        })
    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse cadastrar(@Valid @RequestBody CadastroRequest request) {
        return authService.cadastrar(request);
    }

    @Operation(summary = "Autentica com e-mail e senha e devolve o token de acesso",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login realizado"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos")
        })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Dados do usuário logado", security = @SecurityRequirement(name = OpenApiConfig.BEARER))
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.buscar(Long.valueOf(jwt.getSubject()));
    }
}
