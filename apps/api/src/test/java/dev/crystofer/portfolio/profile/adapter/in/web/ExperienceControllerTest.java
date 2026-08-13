package dev.crystofer.portfolio.profile.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.crystofer.portfolio.profile.adapter.in.web.mapper.ExperienceWebMapperImpl;
import dev.crystofer.portfolio.profile.domain.model.Experience;
import dev.crystofer.portfolio.profile.domain.model.Timeline;
import dev.crystofer.portfolio.profile.domain.port.in.ListExperiencesUseCase;
import dev.crystofer.portfolio.shared.error.GlobalExceptionHandler;

/**
 * Fatia web: sobe o controlador, o conversor de JSON e o tratador de erro, sem banco.
 *
 * <p>O caso de uso e duble; o mapper e o real, porque e ele que decide o formato publicado.
 */
@WebMvcTest(ExperienceController.class)
@Import({ExperienceWebMapperImpl.class, GlobalExceptionHandler.class})
class ExperienceControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean ListExperiencesUseCase listExperiencesUseCase;

  @Test
  @DisplayName("deve devolver a timeline como array json, na ordem que o dominio estabeleceu")
  void shouldReturnTimelineAsArray_whenUseCaseSucceeds() throws Exception {
    // given
    given(listExperiencesUseCase.listExperiences()).willReturn(umaTimeline());

    // when / then
    mockMvc
        .perform(get("/api/v1/experiences"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].company").value("Empresa Atual"))
        .andExpect(jsonPath("$[0].role").value("Desenvolvedor Backend"))
        .andExpect(jsonPath("$[0].startDate").value("2022-08-01"))
        .andExpect(jsonPath("$[0].highlights[0]").value("Migrou o deploy para conteiner"))
        .andExpect(jsonPath("$[1].company").value("Empresa Antiga"))
        .andExpect(jsonPath("$[1].endDate").value("2021-07-31"));
  }

  /**
   * O cargo atual publica {@code endDate} como nulo explicito, e nao como chave omitida.
   *
   * <p>A assercao e sobre o texto cru de proposito. O {@code jsonPath(...).doesNotExist()} do
   * Spring tambem passa quando o valor e nulo, entao ele nao distingue as duas coisas - e a
   * distincao e o contrato aqui: o schema declara {@code endDate} como {@code required} com tipo
   * {@code ["string","null"]}, e e a ausencia de valor que significa cargo atual. Omitir a chave
   * faria o cliente tipado tratar como campo faltante o dado mais importante da timeline.
   */
  @Test
  @DisplayName("deve publicar endDate como null explicito no cargo atual")
  void shouldPublishExplicitNullEndDate_whenRoleIsCurrent() throws Exception {
    // given
    given(listExperiencesUseCase.listExperiences()).willReturn(umaTimeline());

    // when / then
    mockMvc
        .perform(get("/api/v1/experiences"))
        .andExpect(content().string(containsString("\"endDate\":null")));
  }

  /**
   * Timeline vazia e 200 com array vazio, nunca 404.
   *
   * <p>Ausencia de conteudo nao e ausencia de recurso. Este e o estado real do projeto hoje, e um
   * 404 faria o front tratar como erro o portfolio de quem ainda nao preencheu a propria historia.
   */
  @Test
  @DisplayName("deve responder 200 com array vazio quando nao ha passagens")
  void shouldReturnEmptyArray_whenTimelineIsEmpty() throws Exception {
    // given
    given(listExperiencesUseCase.listExperiences()).willReturn(Timeline.empty());

    // when / then
    mockMvc
        .perform(get("/api/v1/experiences"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  @DisplayName("deve declarar o mesmo frescor de cache do perfil")
  void shouldSetCacheControl_whenResponding() throws Exception {
    // given
    given(listExperiencesUseCase.listExperiences()).willReturn(umaTimeline());

    // when / then
    mockMvc
        .perform(get("/api/v1/experiences"))
        .andExpect(
            header().string("Cache-Control", "max-age=300, public, stale-while-revalidate=3600"));
  }

  /** Lista vazia de destaques atravessa como {@code []}, e nao como {@code null}. */
  @Test
  @DisplayName("deve publicar destaques vazios como array vazio")
  void shouldPublishEmptyHighlightsAsArray_whenThereAreNone() throws Exception {
    // given
    var semDestaques =
        new Experience("Acme", "Dev", LocalDate.of(2024, 1, 1), null, "Descricao", List.of());
    given(listExperiencesUseCase.listExperiences()).willReturn(new Timeline(List.of(semDestaques)));

    // when / then
    mockMvc
        .perform(get("/api/v1/experiences"))
        .andExpect(jsonPath("$[0].highlights").isArray())
        .andExpect(jsonPath("$[0].highlights").isEmpty());
  }

  private static Timeline umaTimeline() {
    return new Timeline(
        List.of(
            new Experience(
                "Empresa Antiga",
                "Desenvolvedor Junior",
                LocalDate.of(2019, 2, 1),
                LocalDate.of(2021, 7, 31),
                "Manutencao de servicos",
                List.of("Reduziu a latencia em 40%")),
            new Experience(
                "Empresa Atual",
                "Desenvolvedor Backend",
                LocalDate.of(2022, 8, 1),
                null,
                "Servicos em Java e Spring",
                List.of("Migrou o deploy para conteiner"))));
  }
}
