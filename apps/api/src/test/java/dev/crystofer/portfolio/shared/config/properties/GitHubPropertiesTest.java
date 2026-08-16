package dev.crystofer.portfolio.shared.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GitHubPropertiesTest {

  @Test
  @DisplayName("deve recusar o boot quando nao ha nome de usuario")
  void shouldReject_whenUsernameIsMissing() {
    assertThatThrownBy(() -> properties("  ", "", 30, 20))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GITHUB_USERNAME nao definida");
  }

  /**
   * O limite de repositorios por pagina e do proprio GitHub.
   *
   * <p>Pedir mais que 100 nao devolve mais que 100 - devolve 100 em silencio, e a configuracao
   * passaria a mentir sobre o que o sistema faz.
   */
  @Test
  @DisplayName("deve recusar mais repositorios do que uma pagina comporta")
  void shouldReject_whenRepositoriesExceedOnePage() {
    assertThatThrownBy(() -> properties("CrySamuel", "", 101, 20))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("repositories-to-load");
  }

  /**
   * Somar linguagens de mais repositorios do que os carregados nao e so inutil.
   *
   * <p>Cada repositorio custa uma requisicao a mais, e o numero existe justamente para limitar isso
   * - um valor maior que o outro daria a impressao de um teto que nao existe.
   */
  @Test
  @DisplayName("deve recusar somar linguagens de mais repositorios do que os carregados")
  void shouldReject_whenLanguageLimitExceedsLoaded() {
    assertThatThrownBy(() -> properties("CrySamuel", "", 10, 11))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("repositories-for-languages");
  }

  @Test
  @DisplayName("deve aceitar zero repositorios para linguagens")
  void shouldAccept_zeroRepositoriesForLanguages() {
    assertThat(properties("CrySamuel", "", 10, 0).repositoriesForLanguages()).isZero();
  }

  @Test
  @DisplayName("deve aparar espacos do usuario e do token")
  void shouldTrim_usernameAndToken() {
    GitHubProperties propriedades = properties("  CrySamuel  ", "  ghp_x  ", 30, 20);

    assertThat(propriedades.username()).isEqualTo("CrySamuel");
    assertThat(propriedades.token()).isEqualTo("ghp_x");
  }

  /** E o que decide se a consulta de contribuicoes e tentada - o GraphQL nao aceita anonimo. */
  @Test
  @DisplayName("deve distinguir token ausente de token cadastrado")
  void shouldTell_whenThereIsAToken() {
    assertThat(properties("CrySamuel", "", 30, 20).hasToken()).isFalse();
    assertThat(properties("CrySamuel", "   ", 30, 20).hasToken()).isFalse();
    assertThat(properties("CrySamuel", null, 30, 20).hasToken()).isFalse();
    assertThat(properties("CrySamuel", "ghp_x", 30, 20).hasToken()).isTrue();
  }

  private static GitHubProperties properties(
      String username, String token, int repositoriesToLoad, int repositoriesForLanguages) {
    return new GitHubProperties(
        username,
        token,
        "https://api.github.com",
        "https://api.github.com/graphql",
        Duration.ofSeconds(2),
        Duration.ofSeconds(3),
        repositoriesToLoad,
        repositoriesForLanguages,
        1);
  }
}
