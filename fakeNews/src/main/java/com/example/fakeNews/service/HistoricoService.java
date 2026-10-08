package com.example.fakeNews.service;

import com.example.fakeNews.dto.AnaliseResponse;
import com.example.fakeNews.dto.HistoricoItemResponse;
import com.example.fakeNews.dto.NotaRequest;
import com.example.fakeNews.dto.PaginaResponse;
import com.example.fakeNews.dto.ValidacaoRequest;
import com.example.fakeNews.entity.Analise;
import com.example.fakeNews.entity.Nota;
import com.example.fakeNews.exception.RecursoNaoEncontradoException;
import com.example.fakeNews.exception.RequisicaoInvalidaException;
import com.example.fakeNews.repository.AnaliseRepository;
import com.example.fakeNews.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Histórico de análises (RF03) e validação/anotação pessoal do resultado (Seção IV.H).
 *
 * A gravação é disparada em segundo plano (RNF02 / Fig. 2 "Sincronismo Paralelo
 * Fork-Join") para não atrasar a resposta já devolvida ao usuário; a leitura é
 * paginada (Seção IV.D.4). Cada usuário só enxerga e altera as próprias análises.
 */
@Service
public class HistoricoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 50;

    private static final Set<String> VEREDITOS = Set.of("VERDADEIRO", "FALSO", "INCONCLUSIVO");

    private static final Logger log = LoggerFactory.getLogger(HistoricoService.class);

    private final AnaliseRepository analiseRepository;
    private final UsuarioRepository usuarioRepository;

    public HistoricoService(AnaliseRepository analiseRepository, UsuarioRepository usuarioRepository) {
        this.analiseRepository = analiseRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * @param usuarioId dono da análise, ou null se ela foi feita sem login
     *                  (nesse caso fica apenas como log de auditoria).
     */
    @Async("taskExecutor")
    public void salvarAnaliseAsync(String conteudoOriginal, String origem, String modelo,
                                   Long usuarioId, AnaliseResponse resultado) {
        try {
            Analise analise = new Analise();
            analise.setId(resultado.getId());
            analise.setUsuario(usuarioId != null ? usuarioRepository.getReferenceById(usuarioId) : null);
            analise.setConteudoOriginal(conteudoOriginal.trim());
            analise.setOrigem(origem);
            analise.setModelo(modelo);
            analise.setVeredito(resultado.getVeredito());
            analise.setProbabilidadeFake(resultado.getProbabilidadeFake());
            analise.setJustificativa(resultado.getJustificativa());
            analise.setIndicadores(resultado.getIndicadores() != null
                    ? new ArrayList<>(resultado.getIndicadores())
                    : new ArrayList<>());
            analise.setDataAnalise(resultado.getDataAnalise());

            Analise salva = analiseRepository.save(analise);
            log.info("[HISTORICO] análise {} salva | usuario={} | origem={} | modelo={} | veredito={} | probabilidadeFake={}%",
                    salva.getId(), usuarioId != null ? usuarioId : "anônimo", origem, modelo,
                    salva.getVeredito(), salva.getProbabilidadeFake());
        } catch (Exception e) {
            // A resposta já foi entregue ao usuário: uma falha aqui não pode
            // derrubar a requisição, apenas precisa ficar registrada.
            log.error("[HISTORICO] falha ao salvar análise no banco de dados", e);
        }
    }

    @Transactional(readOnly = true)
    public PaginaResponse<HistoricoItemResponse> listar(Long usuarioId, String veredito, String avaliacao,
                                                        int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(
                Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "dataAnalise").and(Sort.by(Sort.Direction.DESC, "id"))
        );

        Page<Analise> page = analiseRepository.buscarDoUsuario(
                usuarioId, normalizarVeredito(veredito), converterAvaliacao(avaliacao), pageRequest);

        return PaginaResponse.de(page, HistoricoItemResponse::de);
    }

    @Transactional(readOnly = true)
    public HistoricoItemResponse buscarPorId(Long usuarioId, UUID id) {
        return HistoricoItemResponse.de(buscarDoUsuario(usuarioId, id));
    }

    @Transactional
    public HistoricoItemResponse validar(Long usuarioId, UUID id, ValidacaoRequest request) {
        Analise analise = buscarDoUsuario(usuarioId, id);

        if (request.concorda()) {
            analise.setAvaliacaoUsuario(Analise.Avaliacao.CONCORDO);
            analise.setVereditoUsuario(analise.getVeredito());
        } else {
            String vereditoUsuario = normalizarVeredito(request.vereditoUsuario());
            if (vereditoUsuario == null) {
                throw new RequisicaoInvalidaException(
                        "Ao discordar, informe qual seria o veredito correto (VERDADEIRO, FALSO ou INCONCLUSIVO).");
            }
            if (vereditoUsuario.equals(analise.getVeredito())) {
                throw new RequisicaoInvalidaException(
                        "O veredito informado é igual ao da IA. Se você concorda, marque \"Concordo\".");
            }
            analise.setAvaliacaoUsuario(Analise.Avaliacao.DISCORDO);
            analise.setVereditoUsuario(vereditoUsuario);
        }
        analise.setDataAvaliacao(LocalDateTime.now());

        return HistoricoItemResponse.de(analise);
    }

    @Transactional
    public HistoricoItemResponse removerValidacao(Long usuarioId, UUID id) {
        Analise analise = buscarDoUsuario(usuarioId, id);
        analise.setAvaliacaoUsuario(null);
        analise.setVereditoUsuario(null);
        analise.setDataAvaliacao(null);
        return HistoricoItemResponse.de(analise);
    }

    @Transactional
    public HistoricoItemResponse adicionarNota(Long usuarioId, UUID id, NotaRequest request) {
        Analise analise = buscarDoUsuario(usuarioId, id);

        Nota nota = new Nota();
        nota.setAnalise(analise);
        nota.setTexto(request.texto().trim());
        nota.setLinkEvidencia(request.linkEvidencia() == null || request.linkEvidencia().isBlank()
                ? null
                : request.linkEvidencia().trim());
        nota.setDataCriacao(LocalDateTime.now());
        analise.getNotas().add(nota);

        // A análise já está gerenciada: o flush persiste a nota pela cascata e ela volta com id na resposta
        analiseRepository.flush();
        return HistoricoItemResponse.de(analise);
    }

    @Transactional
    public HistoricoItemResponse removerNota(Long usuarioId, UUID id, Long notaId) {
        Analise analise = buscarDoUsuario(usuarioId, id);
        boolean removida = analise.getNotas().removeIf(n -> n.getId().equals(notaId));
        if (!removida) {
            throw new RecursoNaoEncontradoException("Anotação não encontrada.");
        }
        return HistoricoItemResponse.de(analise);
    }

    /** Uma análise de outro usuário é tratada como inexistente, para não revelar que ela existe. */
    private Analise buscarDoUsuario(Long usuarioId, UUID id) {
        return analiseRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Análise não encontrada no seu histórico."));
    }

    private static String normalizarVeredito(String veredito) {
        if (veredito == null || veredito.isBlank()) {
            return null;
        }
        String v = veredito.trim().toUpperCase(Locale.ROOT);
        if (!VEREDITOS.contains(v)) {
            throw new RequisicaoInvalidaException("Veredito inválido: use VERDADEIRO, FALSO ou INCONCLUSIVO.");
        }
        return v;
    }

    private static Analise.Avaliacao converterAvaliacao(String avaliacao) {
        if (avaliacao == null || avaliacao.isBlank()) {
            return null;
        }
        try {
            return Analise.Avaliacao.valueOf(avaliacao.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("Avaliação inválida: use CONCORDO ou DISCORDO.");
        }
    }
}
