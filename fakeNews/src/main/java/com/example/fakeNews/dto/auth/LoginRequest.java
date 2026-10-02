package com.example.fakeNews.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe seu e-mail.")
        String email,

        @NotBlank(message = "Informe sua senha.")
        String senha
) {

    public LoginRequest {
        email = email != null ? email.trim() : null;
    }
}
