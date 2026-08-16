package dev.crystofer.portfolio.github.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.crystofer.portfolio.github.adapter.in.web.mapper.GitHubWebMapperImpl;
import dev.crystofer.portfolio.github.domain.model.GitHubStats;
import dev.crystofer.portfolio.github.domain.model.LanguageUsage;
import dev.crystofer.portfolio.github.domain.model.RepositorySummary;
import dev.crystofer.portfolio.github.domain.port.in.GetGitHubStatsUseCase;
import dev.crystofer.portfolio.shared.config.properties.GitHubProperties;

@WebMvcTest(GitHubController.class)
@Import({GitHubWebMapperImpl.class, GitHubControllerTest.Propriedades.class})
class GitHubControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean GetGitHubStatsUseCase getGitHubStats;

  @Test
  @DisplayName("deve responder o retrato com as fatias em porcentagem")
  void shouldRespond_withSharesInPercent() throws Exception {
    given(getGitHubStats.getGitHubStats())
        .willReturn(
            new GitHubStats(
                "CrySamuel",
                17,
                240,
                List.of(new LanguageUsage("Java", 750_000), new LanguageUsage("Python", 250_000)),
                List.of(repositorio("portfolio", 3))));

    mockMvc
        .perform(get("/api/v1/github/stats"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value("CrySamuel"))
        .andExpect(jsonPath("$.publicRepositories").value(17))
        .andExpect(jsonPath("$.contributionsLastYear").value(240))
        .andExpect(jsonPath("$.languages[0].name").value("Java"))
        .andExpect(jsonPath("$.languages[0].share").value(75.0))
        .andExpect(jsonPath("$.languages[1].share").value(25.0))
        .andExpect(jsonPath("$.repositories[0].name").value("portfolio"));
  }

  /**
   * O peso interno nao vaza para o contrato.
   *
   * <p>Ele e uma unidade do dominio - um milhao de pontos por repositorio - e nao significa nada
   * fora dali. Publicar o numero obrigaria o cliente a somar tudo e dividir para chegar a fatia que
   * este lado ja calculou.
   */
  @Test
  @DisplayName("nao deve publicar o peso interno das linguagens")
  void shouldNotPublish_internalWeight() throws Exception {
    given(getGitHubStats.getGitHubStats())
        .willReturn(
            new GitHubStats(
                "CrySamuel", 1, 0, List.of(new LanguageUsage("Java", 1_000)), List.of()));

    mockMvc
        .perform(get("/api/v1/github/stats"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.not(Matchers.containsString("weight"))))
        .andExpect(jsonPath("$.languages[0].share").value(100.0));
  }

  /**
   * O corte da vitrine vem da configuracao, e nao do dominio.
   *
   * <p>Quantos cards cabem e decisao de tela; o que "destaque" significa e decisao de negocio. As
   * propriedades deste teste publicam dois, e o retrato traz tres.
   */
  @Test
  @DisplayName("deve publicar apenas os repositorios que cabem na vitrine")
  void shouldPublish_onlyTheConfiguredHighlights() throws Exception {
    given(getGitHubStats.getGitHubStats())
        .willReturn(
            new GitHubStats(
                "CrySamuel",
                3,
                0,
                List.of(),
                List.of(repositorio("um", 9), repositorio("dois", 5), repositorio("tres", 1))));

    mockMvc
        .perform(get("/api/v1/github/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.repositories.length()").value(2))
        .andExpect(jsonPath("$.repositories[0].name").value("um"))
        .andExpect(jsonPath("$.repositories[1].name").value("dois"));
  }

  /**
   * Retrato vazio e 200, e nao 404 nem 503.
   *
   * <p>E o ADR-0008 aparecendo na borda: GitHub fora do ar nao e erro do portfolio. Quem consome
   * desenha o estado vazio em vez de tratar falha, e a pagina nunca quebra por causa de um
   * terceiro.
   */
  @Test
  @DisplayName("deve responder 200 com o retrato vazio quando o GitHub esta fora")
  void shouldRespondOk_whenStatsAreEmpty() throws Exception {
    given(getGitHubStats.getGitHubStats()).willReturn(GitHubStats.empty("CrySamuel"));

    mockMvc
        .perform(get("/api/v1/github/stats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("CrySamuel"))
        .andExpect(jsonPath("$.publicRepositories").value(0))
        .andExpect(jsonPath("$.languages").isEmpty())
        .andExpect(jsonPath("$.repositories").isEmpty());
  }

  @Test
  @DisplayName("deve declarar o mesmo frescor das demais leituras")
  void shouldDeclare_theSameCacheControl() throws Exception {
    given(getGitHubStats.getGitHubStats()).willReturn(GitHubStats.empty("CrySamuel"));

    mockMvc
        .perform(get("/api/v1/github/stats"))
        .andExpect(
            header().string("Cache-Control", "max-age=300, public, stale-while-revalidate=3600"));
  }

  /**
   * Campo nulavel vem como chave presente com valor nulo.
   *
   * <p>A distincao importa para o cliente TypeScript gerado do contrato: chave obrigatoria com
   * valor nulavel sai como {@code string | null} e obriga quem consome a tratar o caso. A assercao
   * e sobre o texto cru porque {@code jsonPath().doesNotExist()} passa tambem quando o valor e
   * nulo.
   */
  @Test
  @DisplayName("deve publicar descricao e linguagem nulas como chaves presentes")
  void shouldPublish_nullableFieldsAsPresentKeys() throws Exception {
    given(getGitHubStats.getGitHubStats())
        .willReturn(
            new GitHubStats(
                "CrySamuel",
                1,
                0,
                List.of(),
                List.of(
                    new RepositorySummary(
                        "dotfiles",
                        null,
                        "https://github.com/CrySamuel/dotfiles",
                        null,
                        0,
                        LocalDate.of(2026, 8, 1)))));

    mockMvc
        .perform(get("/api/v1/github/stats"))
        .andExpect(content().string(Matchers.containsString("\"description\":null")))
        .andExpect(content().string(Matchers.containsString("\"primaryLanguage\":null")));
  }

  private static RepositorySummary repositorio(String nome, int estrelas) {
    return new RepositorySummary(
        nome,
        "Descricao",
        "https://github.com/CrySamuel/" + nome,
        "Java",
        estrelas,
        LocalDate.of(2026, 8, 1));
  }

  /** Publica dois repositorios, para que o corte da vitrine seja observavel no teste. */
  @TestConfiguration
  static class Propriedades {

    @Bean
    GitHubProperties gitHubProperties() {
      return new GitHubProperties(
          "CrySamuel",
          "",
          "https://api.github.com",
          "https://api.github.com/graphql",
          Duration.ofSeconds(2),
          Duration.ofSeconds(3),
          30,
          20,
          2);
    }
  }
}
