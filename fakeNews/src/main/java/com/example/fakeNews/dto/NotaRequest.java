package com.example.fakeNews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotaRequest(
        @NotBlank(message = "A anotação não pode estar vazia.")
        @Size(max = 2000, message = "A anotação deve ter no máximo 2000 caracteres.")
        String texto,

        @Size(max = 2048, message = "O link deve ter no máximo 2048 caracteres.")
        @Pattern(regexp = "^(https?://\\S+)?$", message = "O link de evidência deve começar com http:// ou https://.")
        String linkEvidencia
) {
}
