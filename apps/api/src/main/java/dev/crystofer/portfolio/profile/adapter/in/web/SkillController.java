package dev.crystofer.portfolio.profile.adapter.in.web;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.SkillCategoryResponse;
import dev.crystofer.portfolio.profile.adapter.in.web.mapper.SkillWebMapper;
import dev.crystofer.portfolio.profile.domain.port.in.ListSkillsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Adaptador de entrada HTTP das competencias.
 *
 * <p><strong>O agrupamento chega pronto, e isso e o titulo do commit.</strong> A F05 determina que
 * agrupar e regra de negocio, nao formatacao - o cliente recebe grupos e desenha grupos, sem
 * precisar percorrer uma lista plana decidindo onde comeca cada cabecalho. Dois clientes fariam
 * esse agrupamento de dois jeitos.
 *
 * <p>A ordem tambem nao e decidida aqui: chega do dominio e este arquivo so a preserva.
 */
@RestController
@RequestMapping("/api/v1/skills")
@Tag(name = "Skill", description = "Competencias tecnicas agrupadas por categoria")
public class SkillController {

  /** O mesmo frescor do perfil e da timeline: conteudo que muda por migracao e deploy. */
  private static final CacheControl CACHE =
      CacheControl.maxAge(Duration.ofMinutes(5))
          .cachePublic()
          .staleWhileRevalidate(Duration.ofHours(1));

  private final ListSkillsUseCase listSkillsUseCase;
  private final SkillWebMapper mapper;

  public SkillController(ListSkillsUseCase listSkillsUseCase, SkillWebMapper mapper) {
    this.listSkillsUseCase = listSkillsUseCase;
    this.mapper = mapper;
  }

  /**
   * Array de categorias, e nao objeto envelopando a lista.
   *
   * <p>A ordem do array e o contrato, como ja e para {@code socialLinks} e para a timeline. Manter
   * as tres colecoes desta API com a mesma forma vale mais do que a evolutibilidade de um envelope.
   *
   * <p>Catalogo vazio responde 200 com {@code []}, nunca 404: ausencia de conteudo nao e ausencia
   * de recurso, e hoje esse e o estado real - o dono ainda nao informou os niveis.
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Lista as competencias agrupadas por categoria",
      description =
          "Categorias em ordem editorial; dentro de cada uma, competencias do maior nivel para o "
              + "menor. Categoria sem competencia nao aparece.")
  @ApiResponse(responseCode = "200", description = "Catalogo, possivelmente vazio")
  public ResponseEntity<List<SkillCategoryResponse>> listSkills() {
    var catalog = listSkillsUseCase.listSkills();
    return ResponseEntity.ok().cacheControl(CACHE).body(mapper.toResponse(catalog.categories()));
  }
}
