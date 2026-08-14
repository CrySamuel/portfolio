package dev.crystofer.portfolio.projects.adapter.in.web;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.crystofer.portfolio.projects.adapter.in.web.dto.ProjectDetailResponse;
import dev.crystofer.portfolio.projects.adapter.in.web.dto.ProjectSummaryResponse;
import dev.crystofer.portfolio.projects.adapter.in.web.mapper.ProjectWebMapper;
import dev.crystofer.portfolio.projects.domain.port.in.GetProjectBySlugUseCase;
import dev.crystofer.portfolio.projects.domain.port.in.ListProjectsUseCase;
import dev.crystofer.portfolio.shared.domain.Slug;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Adaptador de entrada HTTP do catalogo de projetos.
 *
 * <p>Depende das interfaces do dominio, nunca de {@code ProjectService}. O controlador nao decide
 * nada de negocio: pede, converte e responde.
 *
 * <p><strong>A ordem nao e decidida aqui</strong> - chega pronta do {@code ProjectCatalog}, e este
 * arquivo so a preserva. Um {@code sort} neste metodo seria um segundo lugar decidindo a mesma
 * coisa.
 *
 * <p>O ETag das duas respostas nao e montado aqui e sim pelo filtro registrado em {@code
 * HttpCacheConfig}, que vale para a API inteira.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(
    name = "Project",
    description = "Catalogo de projetos com narrativa problema, solucao e resultado")
public class ProjectController {

  /** O mesmo frescor das demais leituras: conteudo que muda por migracao e deploy (secao 3.8). */
  private static final CacheControl CACHE =
      CacheControl.maxAge(Duration.ofMinutes(5))
          .cachePublic()
          .staleWhileRevalidate(Duration.ofHours(1));

  private final ListProjectsUseCase listProjectsUseCase;
  private final GetProjectBySlugUseCase getProjectBySlugUseCase;
  private final ProjectWebMapper mapper;

  public ProjectController(
      ListProjectsUseCase listProjectsUseCase,
      GetProjectBySlugUseCase getProjectBySlugUseCase,
      ProjectWebMapper mapper) {
    this.listProjectsUseCase = listProjectsUseCase;
    this.getProjectBySlugUseCase = getProjectBySlugUseCase;
    this.mapper = mapper;
  }

  /**
   * A resposta e um array puro, e nao um objeto envelopando a lista.
   *
   * <p>Mesma forma das outras colecoes desta API, pela razao ja registrada: a ordem do array e o
   * contrato, e nao ha metadado a carregar. O filtro por tecnologia do commit 38 e resolvido no
   * cliente, sobre a lista inteira, sem requisicao de rede - entao nao ha paginacao nem contagem
   * para um envelope transportar.
   *
   * <p><strong>Catalogo vazio responde 200 com {@code []}, nunca 404.</strong> Ausencia de conteudo
   * nao e ausencia de recurso, e este e o estado real deste projeto ate o seed chegar.
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Lista os projetos do catalogo",
      description =
          "Projetos em ordem editorial. Sem a narrativa e sem os enderecos, que sao do detalhe.")
  @ApiResponse(responseCode = "200", description = "Catalogo, possivelmente vazio")
  public ResponseEntity<List<ProjectSummaryResponse>> listProjects() {
    var catalogo = listProjectsUseCase.listProjects();
    return ResponseEntity.ok().cacheControl(CACHE).body(mapper.toSummary(catalogo.projects()));
  }

  /**
   * O slug chega como {@link Slug}, e nao como {@code String}.
   *
   * <p>Quem converte e o {@code ObjectToObjectConverter} do Spring, que encontra a fabrica estatica
   * {@link Slug#of(String)} sozinho - isso foi medido, e nao suposto. Um {@code Converter} proprio
   * chegou a ser escrito e saiu depois da medicao, pela mesma razao que o {@code format = "date"}
   * saiu do DTO de experiencia: anotacao redundante confunde quem le.
   *
   * <p>O efeito e o que interessa: a validacao de formato roda uma vez, na borda, antes de qualquer
   * ida ao banco - e a excecao do value object vira {@code MethodArgumentTypeMismatchException},
   * que o {@code ResponseEntityExceptionHandler} ja traduz em <strong>400</strong>. Sem a conversao
   * na borda, ela cairia no catch-all e viraria 500, dizendo que a aplicacao quebrou quando quem
   * errou foi quem digitou o endereco.
   *
   * <p><strong>A dependencia e do nome do metodo.</strong> Renomear {@code Slug.of} quebraria a
   * conversao, e e por isso que os testes de 400 e de detalhe existem - eles nomeiam essa ligacao.
   *
   * <p>Slug bem formado que nao existe e outro caso, e responde 404: o endereco e valido, o recurso
   * e que nao esta la.
   */
  @GetMapping(path = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Detalha um projeto pelo slug",
      description = "Narrativa completa, enderecos e metricas de um projeto do catalogo.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
    @ApiResponse(responseCode = "400", description = "Slug fora do formato da URL"),
    @ApiResponse(responseCode = "404", description = "Nao ha projeto com esse slug")
  })
  public ResponseEntity<ProjectDetailResponse> getProject(@PathVariable Slug slug) {
    var projeto = getProjectBySlugUseCase.getProjectBySlug(slug);
    return ResponseEntity.ok().cacheControl(CACHE).body(mapper.toDetail(projeto));
  }
}
