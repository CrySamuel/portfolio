package dev.crystofer.portfolio.profile.adapter.in.web;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.crystofer.portfolio.profile.adapter.in.web.dto.ProfileResponse;
import dev.crystofer.portfolio.profile.adapter.in.web.mapper.ProfileWebMapper;
import dev.crystofer.portfolio.profile.domain.port.in.GetProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Adaptador de entrada HTTP do perfil.
 *
 * <p>Depende de {@link GetProfileUseCase}, a interface do dominio - nunca de {@code
 * ProfileService}. E o que permite testar este controlador sem banco e trocar a implementacao do
 * caso de uso sem tocar aqui (secao 13.2, Dependency Inversion).
 *
 * <p>O controlador nao decide nada de negocio: pede o perfil, converte e responde. Ausencia de
 * perfil nao aparece neste arquivo porque virou excecao na camada de aplicacao e resposta no {@code
 * GlobalExceptionHandler} - um lugar cada.
 */
@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Dados publicos do dono do portfolio")
public class ProfileController {

  /**
   * Cinco minutos de frescor, uma hora de tolerancia (secao 3.8).
   *
   * <p>O conteudo muda por migracao e deploy, entao servi-lo por cinco minutos nao arrisca mostrar
   * dado velho de forma relevante. O {@code stale-while-revalidate} e o que importa para o custo:
   * expirado o max-age, a CDN entrega o valor antigo na hora e revalida em segundo plano - o
   * visitante nunca espera pela API, que e a premissa do ADR-0006 para o free tier hibernar sem
   * prejuizo.
   *
   * <p>{@code ETag} ainda nao entra: e filtro de aplicacao inteira e o plano o agenda no commit 36,
   * junto dos endpoints de listagem, onde passa a valer a pena.
   */
  private static final CacheControl CACHE =
      CacheControl.maxAge(Duration.ofMinutes(5))
          .cachePublic()
          .staleWhileRevalidate(Duration.ofHours(1));

  private final GetProfileUseCase getProfileUseCase;
  private final ProfileWebMapper mapper;

  public ProfileController(GetProfileUseCase getProfileUseCase, ProfileWebMapper mapper) {
    this.getProfileUseCase = getProfileUseCase;
    this.mapper = mapper;
  }

  @GetMapping
  @Operation(
      summary = "Devolve o perfil publico",
      description = "Nome, headline, bio, disponibilidade e perfis externos, em ordem de exibicao.")
  @ApiResponse(responseCode = "200", description = "Perfil encontrado")
  @ApiResponse(
      responseCode = "404",
      description = "Nenhum perfil cadastrado - o seed nao foi aplicado",
      content = @io.swagger.v3.oas.annotations.media.Content)
  public ResponseEntity<ProfileResponse> getProfile() {
    var profile = getProfileUseCase.getProfile();
    return ResponseEntity.ok().cacheControl(CACHE).body(mapper.toResponse(profile));
  }
}
