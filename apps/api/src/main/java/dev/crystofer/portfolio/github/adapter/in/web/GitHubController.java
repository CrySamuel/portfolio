package dev.crystofer.portfolio.github.adapter.in.web;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.crystofer.portfolio.github.adapter.in.web.dto.GitHubStatsResponse;
import dev.crystofer.portfolio.github.adapter.in.web.mapper.GitHubWebMapper;
import dev.crystofer.portfolio.github.domain.port.in.GetGitHubStatsUseCase;
import dev.crystofer.portfolio.shared.config.properties.GitHubProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * As estatisticas do perfil publico do GitHub.
 *
 * <p><strong>Este endpoint nao consta da secao 16 do plano</strong>, e a ausencia parece descuido e
 * nao decisao: a secao 1.5 o lista no escopo da API, o commit 44 o consome no site, e todos os
 * outros modulos ganharam um commit proprio de "expose GET ...". Ele entra seguindo o mesmo padrao
 * dos irmaos.
 *
 * <p><strong>Nao ha caminho de erro aqui, e isso e o ADR-0008 aparecendo na borda.</strong> As
 * outras rotas desta API podem responder 404 ou 400; esta responde 200 sempre. GitHub fora do ar
 * devolve o retrato vazio - a cadeia de fallback termina no adaptador, muito antes de chegar ao
 * controlador -, e quem consome desenha o estado vazio em vez de tratar erro.
 *
 * <p>O caminho e {@code /api/v1/github/stats}, no singular do recurso agregado: nao e uma colecao
 * de estatisticas, e um retrato so. Por isso tambem nao ha array na raiz, ao contrario das outras
 * tres leituras.
 */
@RestController
@RequestMapping("/api/v1/github")
@Tag(name = "GitHub", description = "Estatisticas do perfil publico, com cache e fallback")
public class GitHubController {

  /**
   * O mesmo frescor das demais leituras, e nao as seis horas do cache interno.
   *
   * <p>Sao prazos de coisas diferentes: o cache da aplicacao decide quando <em>buscar</em> no
   * GitHub, e este cabecalho decide por quanto tempo o BFF e a CDN podem <em>reusar</em> a
   * resposta. Alinhar os dois faria uma revalidacao do site chegar minutos depois de o cache
   * expirar, sem ninguem ganhar nada.
   */
  private static final CacheControl CACHE =
      CacheControl.maxAge(Duration.ofMinutes(5))
          .cachePublic()
          .staleWhileRevalidate(Duration.ofHours(1));

  private final GetGitHubStatsUseCase getGitHubStats;
  private final GitHubWebMapper mapper;
  private final GitHubProperties properties;

  public GitHubController(
      GetGitHubStatsUseCase getGitHubStats, GitHubWebMapper mapper, GitHubProperties properties) {
    this.getGitHubStats = getGitHubStats;
    this.mapper = mapper;
    this.properties = properties;
  }

  @GetMapping(path = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Estatisticas do perfil publico no GitHub",
      description =
          "Linguagens, repositorios em destaque e contadores do perfil. Responde 200 mesmo com o"
              + " GitHub indisponivel, devolvendo o retrato vazio.")
  @ApiResponse(responseCode = "200", description = "Retrato do perfil, possivelmente vazio")
  public ResponseEntity<GitHubStatsResponse> getStats() {
    var stats = getGitHubStats.getGitHubStats();
    return ResponseEntity.ok()
        .cacheControl(CACHE)
        .body(mapper.toResponse(stats, properties.repositoriesToPublish()));
  }
}
