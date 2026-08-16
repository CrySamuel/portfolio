package dev.crystofer.portfolio.github.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.crystofer.portfolio.github.domain.model.GitHubStats;
import dev.crystofer.portfolio.github.domain.port.out.GitHubStatsProviderPort;
import dev.crystofer.portfolio.shared.config.properties.GitHubProperties;

class GitHubStatsServiceTest {

  private final GitHubStatsProviderPort provider = mock(GitHubStatsProviderPort.class);

  /**
   * O servico consulta o perfil da configuracao, e nao um nome recebido de fora.
   *
   * <p>E a razao de ele existir: a porta de entrada nao tem parametro justamente para que a API nao
   * vire um proxy publico da API do GitHub, com qualquer um consultando qualquer perfil e gastando
   * a cota do token do dono.
   */
  @Test
  @DisplayName("deve consultar o perfil configurado")
  void shouldQuery_configuredProfile() {
    when(provider.fetchStats("CrySamuel")).thenReturn(GitHubStats.empty("CrySamuel"));

    var stats = new GitHubStatsService(provider, propriedades("CrySamuel")).getGitHubStats();

    assertThat(stats.username()).isEqualTo("CrySamuel");
  }

  /**
   * Retrato vazio atravessa o servico sem virar excecao.
   *
   * <p>O oposto do {@code ProfileService}, que lanca quando nao ha perfil - e a diferenca e de
   * significado: perfil ausente e sistema quebrado, GitHub fora do ar e terca-feira.
   */
  @Test
  @DisplayName("nao deve transformar retrato vazio em erro")
  void shouldNotTurn_emptyStatsIntoError() {
    when(provider.fetchStats(any())).thenReturn(GitHubStats.empty("CrySamuel"));

    var stats = new GitHubStatsService(provider, propriedades("CrySamuel")).getGitHubStats();

    assertThat(stats.isEmpty()).isTrue();
    assertThat(stats.languages()).isEmpty();
  }

  @Test
  @DisplayName("deve devolver o retrato como a porta o entrega, sem reordenar")
  void shouldReturn_statsUntouched() {
    var completo = new GitHubStats("CrySamuel", 17, 240, List.of(), List.of());
    when(provider.fetchStats("CrySamuel")).thenReturn(completo);

    var stats = new GitHubStatsService(provider, propriedades("CrySamuel")).getGitHubStats();

    assertThat(stats).isEqualTo(completo);
  }

  private static GitHubProperties propriedades(String username) {
    return new GitHubProperties(
        username,
        "",
        "https://api.github.com",
        "https://api.github.com/graphql",
        Duration.ofSeconds(2),
        Duration.ofSeconds(3),
        30,
        20,
        6);
  }
}
