package dev.crystofer.portfolio.profile.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.crystofer.portfolio.profile.adapter.in.web.mapper.ProfileWebMapperImpl;
import dev.crystofer.portfolio.profile.domain.model.Profile;
import dev.crystofer.portfolio.profile.domain.model.SocialLink;
import dev.crystofer.portfolio.profile.domain.model.SocialPlatform;
import dev.crystofer.portfolio.profile.domain.port.in.GetProfileUseCase;
import dev.crystofer.portfolio.shared.error.GlobalExceptionHandler;
import dev.crystofer.portfolio.shared.error.ResourceNotFoundException;

/**
 * Fatia web (secao 4.3): sobe o controlador, o conversor de JSON e o tratador de erro, sem banco e
 * sem o resto do contexto.
 *
 * <p>O caso de uso e duble; o mapper e o real, porque e ele que decide o formato publicado - trocar
 * por mock testaria o teste.
 */
@WebMvcTest(ProfileController.class)
@Import({ProfileWebMapperImpl.class, GlobalExceptionHandler.class})
class ProfileControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean GetProfileUseCase getProfileUseCase;

  @Test
  @DisplayName("deve devolver o perfil em json com os campos do contrato")
  void shouldReturnProfile_whenUseCaseSucceeds() throws Exception {
    // given
    given(getProfileUseCase.getProfile()).willReturn(umPerfil());

    // when / then
    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.fullName").value("Crystofer Demetino"))
        .andExpect(jsonPath("$.location").value("Remoto · Brasil"))
        .andExpect(jsonPath("$.availableForWork").value(true))
        .andExpect(jsonPath("$.socialLinks[0].platform").value("github"))
        .andExpect(jsonPath("$.socialLinks[1].platform").value("email"));
  }

  /**
   * Campo opcional ausente vira {@code null} explicito, e nao chave omitida.
   *
   * <p>A assercao e sobre o texto cru de proposito. O {@code jsonPath(...).doesNotExist()} do
   * Spring tambem passa quando o valor e nulo, entao ele nao distingue as duas coisas - e a
   * distincao e justamente o que este teste existe para fixar. Um cliente tipado gerado do OpenAPI
   * trata {@code string | null} diferente de propriedade opcional.
   */
  @Test
  @DisplayName("deve publicar campo opcional vazio como null presente, e nao omitir a chave")
  void shouldSerializeAbsentOptionalAsExplicitNull() throws Exception {
    // given
    given(getProfileUseCase.getProfile()).willReturn(umPerfil());

    // when / then
    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"resumeUrl\":null")));
  }

  @Test
  @DisplayName("nao deve expor displayOrder - a ordem do array e o contrato")
  void shouldNotExposeDisplayOrder_whenSerializing() throws Exception {
    // given
    given(getProfileUseCase.getProfile()).willReturn(umPerfil());

    // when / then
    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.socialLinks[0].displayOrder").doesNotExist());
  }

  @Test
  @DisplayName("deve mandar o Cache-Control da secao 3.8")
  void shouldSendCacheControl_whenProfileIsFound() throws Exception {
    // given
    given(getProfileUseCase.getProfile()).willReturn(umPerfil());

    // when / then
    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", "max-age=300, public, stale-while-revalidate=3600"));
  }

  @Test
  @DisplayName("deve responder 404 em problem+json quando nao houver perfil")
  void shouldReturnProblemDetail_whenProfileIsMissing() throws Exception {
    // given
    given(getProfileUseCase.getProfile())
        .willThrow(new ResourceNotFoundException("profile", "Perfil nao encontrado"));

    // when / then
    mockMvc
        .perform(get("/api/v1/profile"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.title").value("Recurso nao encontrado"))
        .andExpect(jsonPath("$.detail").value("Perfil nao encontrado"))
        .andExpect(jsonPath("$.type").value("/errors/resource-not-found"));
  }

  private static Profile umPerfil() {
    return new Profile(
        "Crystofer Demetino",
        "Desenvolvedor Backend",
        "Bio",
        "Remoto · Brasil",
        null,
        true,
        List.of(
            new SocialLink(SocialPlatform.GITHUB, "https://github.com/CrySamuel", 0),
            new SocialLink(SocialPlatform.EMAIL, "mailto:a@b.com", 1)));
  }
}
