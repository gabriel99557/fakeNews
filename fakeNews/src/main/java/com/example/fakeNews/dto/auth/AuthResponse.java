package com.example.fakeNews.dto.auth;

import java.time.Instant;

/** Resposta de cadastro/login: o token JWT deve ser enviado no header "Authorization: Bearer <token>". */
public record AuthResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
}
