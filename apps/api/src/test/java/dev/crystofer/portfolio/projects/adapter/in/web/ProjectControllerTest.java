package dev.crystofer.portfolio.projects.adapter.in.web;

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

import dev.crystofer.portfolio.projects.adapter.in.web.mapper.ProjectWebMapperImpl;
import dev.crystofer.portfolio.projects.domain.model.Project;
import dev.crystofer.portfolio.projects.domain.model.ProjectCatalog;
import dev.crystofer.portfolio.projects.domain.port.in.GetProjectBySlugUseCase;
import dev.crystofer.portfolio.projects.domain.port.in.ListProjectsUseCase;
import dev.crystofer.portfolio.shared.domain.Slug;
import dev.crystofer.portfolio.shared.error.GlobalExceptionHandler;
import dev.crystofer.portfolio.shared.error.ResourceNotFoundException;
import dev.crystofer.portfolio.support.fixtures.ProjectSamples;

@WebMvcTest(ProjectController.class)
@Import({ProjectWebMapperImpl.class, GlobalExceptionHandler.class})
class ProjectControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean ListProjectsUseCase listProjectsUseCase;

  @MockitoBean GetProjectBySlugUseCase getProjectBySlugUseCase;

  @Test
  @DisplayName("deve responder a listagem como array puro")
  void shouldRespondListing_asABareArray() throws Exception {
    // given
    given(listProjectsUseCase.listProjects())
        .willReturn(
            new ProjectCatalog(
                List.of(
                    ProjectSamples.projeto("finai", "FinAI", 0, true),
                    ProjectSamples.projeto("portfolio", "Portfolio", 1, false))));

    // when / then
    mockMvc
        .perform(get("/api/v1/projects"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[0].slug").value("finai"))
        .andExpect(jsonPath("$[1].slug").value("portfolio"))
        .andExpect(header().string("Cache-Control", containsString("max-age=300")));
  }

  /**
   * O resumo nao publica a narrativa nem os enderecos, e a assercao e sobre o texto cru.
   *
   * <p>Um {@code jsonPath(...).doesNotExist()} passaria tambem se o campo viesse como {@code null},
   * que e a armadilha ja registrada. Aqui o que se quer provar e que a chave <strong>nao
   * existe</strong> no corpo.
   */
  @Test
  @DisplayName("deve omitir narrativa e enderecos do resumo")
  void shouldOmitNarrativeAndUrls_fromTheSummary() throws Exception {
    given(listProjectsUseCase.listProjects())
        .willReturn(new ProjectCatalog(List.of(ProjectSamples.projetoCompleto())));

    var corpo =
        mockMvc
            .perform(get("/api/v1/projects"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(corpo)
        .doesNotContain("problem", "solution", "outcome", "repoUrl", "liveUrl", "metrics")
        .contains("\"slug\":\"finai\"", "\"technologies\"");
  }

  @Test
  @DisplayName("deve responder o detalhe com a narrativa completa")
  void shouldRespondDetail_withTheFullNarrative() throws Exception {
    // given
    Project projeto = ProjectSamples.projetoCompleto();
    given(getProjectBySlugUseCase.getProjectBySlug(Slug.of("finai"))).willReturn(projeto);

    // when / then
    mockMvc
        .perform(get("/api/v1/projects/finai"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slug").value("finai"))
        .andExpect(jsonPath("$.problem").value("O problema."))
        .andExpect(jsonPath("$.solution").value("A solucao."))
        .andExpect(jsonPath("$.outcome").value("O resultado."))
        .andExpect(jsonPath("$.repoUrl").value("https://github.com/CrySamuel/FinAI-Bot"))
        .andExpect(jsonPath("$.metrics[0].label").value("Economia em um mes"))
        .andExpect(jsonPath("$.technologies[0].name").value("Oracle Cloud"))
        .andExpect(jsonPath("$.technologies[0].category").value("infrastructure"));
  }

  /**
   * Campo nulavel vem presente com valor nulo, e a assercao e sobre o texto cru pela mesma razao.
   *
   * <p>E a diferenca entre "a chave nao veio" e "a chave veio vazia" que o cliente tipado precisa
   * enxergar - e o {@code jsonPath} nao a enxerga.
   */
  @Test
  @DisplayName("deve publicar os nulaveis presentes e nulos")
  void shouldPublishNullable_asPresentAndNull() throws Exception {
    given(getProjectBySlugUseCase.getProjectBySlug(Slug.of("interno")))
        .willReturn(ProjectSamples.projeto("interno", "Interno", 0, false));

    mockMvc
        .perform(get("/api/v1/projects/interno"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"repoUrl\":null")))
        .andExpect(content().string(containsString("\"liveUrl\":null")))
        .andExpect(content().string(containsString("\"coverImage\":null")))
        .andExpect(content().string(containsString("\"publishedAt\":null")));
  }

  @Test
  @DisplayName("deve responder 404 em problem+json quando o slug nao existe")
  void shouldRespond404_whenSlugIsUnknown() throws Exception {
    given(getProjectBySlugUseCase.getProjectBySlug(Slug.of("nao-existe")))
        .willThrow(new ResourceNotFoundException("project", "Projeto nao encontrado: nao-existe"));

    mockMvc
        .perform(get("/api/v1/projects/nao-existe"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.detail").value(containsString("nao-existe")));
  }

  /**
   * Slug malformado e erro do cliente, e o status precisa dizer isso.
   *
   * <p>Quem converte o trecho da URL no value object e o {@code ObjectToObjectConverter} do Spring,
   * que encontra {@code Slug.of} por convencao de nome. Este teste e o que guarda essa ligacao
   * invisivel: renomear a fabrica quebraria a conversao, e sem ele a {@code
   * IllegalArgumentException} passaria a cair no catch-all e virar <strong>500</strong> - a
   * aplicacao declarando que quebrou quando quem errou foi quem digitou o endereco.
   */
  @Test
  @DisplayName("deve responder 400 quando o slug esta fora do formato")
  void shouldRespond400_whenSlugIsMalformed() throws Exception {
    mockMvc.perform(get("/api/v1/projects/Slug Invalido")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/projects/MAIUSCULA")).andExpect(status().isBadRequest());
    mockMvc.perform(get("/api/v1/projects/com_underline")).andExpect(status().isBadRequest());
  }
}
