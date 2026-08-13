package dev.crystofer.portfolio.profile.adapter.in.web;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.ExperienceResponse;
import dev.crystofer.portfolio.profile.adapter.in.web.mapper.ExperienceWebMapper;
import dev.crystofer.portfolio.profile.domain.port.in.ListExperiencesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Adaptador de entrada HTTP da timeline.
 *
 * <p>Depende de {@link ListExperiencesUseCase}, a interface do dominio - nunca de {@code
 * ExperienceService}. O controlador nao decide nada de negocio: pede a timeline, converte e
 * responde.
 *
 * <p><strong>A ordem nao e decidida aqui.</strong> Ela chega pronta do dominio, e este arquivo so a
 * preserva. Um {@code sort} neste metodo seria um segundo lugar decidindo a mesma coisa - e o modo
 * pelo qual duas telas do mesmo sistema passam a mostrar ordens diferentes.
 */
@RestController
@RequestMapping("/api/v1/experiences")
@Tag(name = "Experience", description = "Timeline profissional do dono do portfolio")
public class ExperienceController {

  /** O mesmo frescor do perfil: conteudo que muda por migracao e deploy (secao 3.8). */
  private static final CacheControl CACHE =
      CacheControl.maxAge(Duration.ofMinutes(5))
          .cachePublic()
          .staleWhileRevalidate(Duration.ofHours(1));

  private final ListExperiencesUseCase listExperiencesUseCase;
  private final ExperienceWebMapper mapper;

  public ExperienceController(
      ListExperiencesUseCase listExperiencesUseCase, ExperienceWebMapper mapper) {
    this.listExperiencesUseCase = listExperiencesUseCase;
    this.mapper = mapper;
  }

  /**
   * A resposta e um array puro, e nao um objeto envelopando a lista.
   *
   * <p>A ordem do array e o contrato, exatamente como ja e para {@code socialLinks} no perfil - e
   * manter as duas colecoes desta API com a mesma forma vale mais do que a evolutibilidade que um
   * envelope daria. Nao ha metadado a carregar: a timeline nao pagina, nao filtra e nao conta.
   *
   * <p><strong>Timeline vazia responde 200 com {@code []}, nunca 404.</strong> Ausencia de conteudo
   * nao e ausencia de recurso: o recurso "timeline deste portfolio" existe e esta vazio. Um 404
   * faria o front tratar como erro o estado normal de quem ainda nao preencheu a propria historia -
   * que e, hoje, o estado real deste projeto.
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Lista a timeline profissional",
      description =
          "Passagens em ordem cronologica decrescente. Data de saida nula significa cargo atual.")
  @ApiResponse(responseCode = "200", description = "Timeline, possivelmente vazia")
  public ResponseEntity<List<ExperienceResponse>> listExperiences() {
    var timeline = listExperiencesUseCase.listExperiences();
    return ResponseEntity.ok().cacheControl(CACHE).body(mapper.toResponse(timeline.experiences()));
  }
}
