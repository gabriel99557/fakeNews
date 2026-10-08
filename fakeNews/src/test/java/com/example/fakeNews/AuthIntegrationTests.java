package com.example.fakeNews;

import com.example.fakeNews.repository.AnaliseRepository;
import com.example.fakeNews.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cadastro, login e proteção das rotas (Spring Security + JWT). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AnaliseRepository analiseRepository;

    @BeforeEach
    void limparBanco() {
        analiseRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void cadastroCriaContaComSenhaCriptografadaEDevolveToken() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Gabriel", "email": "  Gabriel@Exemplo.com ", "senha": "senhaForte123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.usuario.nome").value("Gabriel"))
                .andExpect(jsonPath("$.usuario.email").value("gabriel@exemplo.com"))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());

        var usuario = usuarioRepository.findByEmail("gabriel@exemplo.com").orElseThrow();
        assertThat(usuario.getSenhaHash()).isNotEqualTo("senhaForte123").startsWith("$2");
    }

    @Test
    void cadastroComEmailRepetidoRetorna409() throws Exception {
        cadastrar("repetido@exemplo.com");

        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Outra pessoa", "email": "REPETIDO@exemplo.com", "senha": "outraSenha123"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Já existe uma conta com este e-mail."));
    }

    @Test
    void cadastroComDadosInvalidosRetorna400() throws Exception {
        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Gabriel", "email": "nao-e-email", "senha": "senhaForte123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe um e-mail válido."));

        mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Gabriel", "email": "a@b.com", "senha": "curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A senha deve ter entre 8 e 72 caracteres."));
    }

    @Test
    void loginComSenhaCorretaDevolveTokenEComSenhaErradaRetorna401() throws Exception {
        cadastrar("login@exemplo.com");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "LOGIN@exemplo.com", "senha": "senhaForte123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyOrNullString())));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "login@exemplo.com", "senha": "senhaErrada"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));

        // E-mail inexistente recebe a mesma mensagem (não revela quais e-mails têm conta)
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ninguem@exemplo.com", "senha": "senhaForte123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
    }

    @Test
    void meRetornaUsuarioDoToken() throws Exception {
        String token = cadastrar("me@exemplo.com");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("me@exemplo.com"));
    }

    @Test
    void rotasProtegidasExigemTokenValido() throws Exception {
        mockMvc.perform(get("/api/analise/historico"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Faça login para acessar este recurso."));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.qualquer"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Sua sessão expirou ou é inválida. Entre novamente."));
    }

    @Test
    void analiseContinuaPublicaSemLogin() throws Exception {
        // Texto curto demais: é barrado pela validação (400), e não pela segurança (401)
        mockMvc.perform(post("/api/analise/texto").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"conteudo\": \"oi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void corsAceitaOFrontendERecusaOutrasOrigens() throws Exception {
        mockMvc.perform(options("/api/analise/historico")
                        .header(HttpHeaders.ORIGIN, "http://localhost:9000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:9000"));

        mockMvc.perform(options("/api/analise/historico")
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rotaInexistenteRetorna404EJsonMalformadoRetorna400() throws Exception {
        String token = cadastrar("erros@exemplo.com");

        mockMvc.perform(get("/api/nao-existe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{json quebrado"))
                .andExpect(status().isBadRequest());
    }

    private String cadastrar(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Teste", "email": "%s", "senha": "senhaForte123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(resposta, "$.token");
    }
}
