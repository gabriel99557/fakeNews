package com.example.fakeNews.service;

import com.example.fakeNews.dto.auth.AuthResponse;
import com.example.fakeNews.dto.auth.CadastroRequest;
import com.example.fakeNews.dto.auth.LoginRequest;
import com.example.fakeNews.dto.auth.UsuarioResponse;
import com.example.fakeNews.entity.Usuario;
import com.example.fakeNews.exception.CredenciaisInvalidasException;
import com.example.fakeNews.exception.EmailJaCadastradoException;
import com.example.fakeNews.exception.RecursoNaoEncontradoException;
import com.example.fakeNews.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/** Cadastro e login de usuários. */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse cadastrar(CadastroRequest request) {
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailJaCadastradoException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setDataCriacao(LocalDateTime.now());

        try {
            usuario = usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Dois cadastros simultâneos com o mesmo e-mail: a restrição UNIQUE do banco decide
            throw new EmailJaCadastradoException("Já existe uma conta com este e-mail.");
        }
        return autenticado(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                // Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails têm conta
                .orElseThrow(() -> new CredenciaisInvalidasException("E-mail ou senha incorretos."));
        return autenticado(usuario);
    }

    public UsuarioResponse buscar(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private AuthResponse autenticado(Usuario usuario) {
        Jwt token = tokenService.gerar(usuario);
        return new AuthResponse(token.getTokenValue(), token.getExpiresAt(), UsuarioResponse.de(usuario));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
