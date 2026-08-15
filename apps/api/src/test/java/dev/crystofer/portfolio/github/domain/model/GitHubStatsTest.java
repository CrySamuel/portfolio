package dev.crystofer.portfolio.github.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GitHubStatsTest {

  private static final String USUARIO = "CrySamuel";

  @Test
  @DisplayName("deve ordenar as linguagens da mais usada para a menos")
  void shouldOrderLanguages_byUsage() {
    GitHubStats stats =
        stats(
            List.of(
                new LanguageUsage("Python", 30_000),
                new LanguageUsage("Java", 90_000),
                new LanguageUsage("TypeScript", 60_000)),
            List.of());

    assertThat(stats.languages())
        .extracting(LanguageUsage::name)
        .containsExactly("Java", "TypeScript", "Python");
  }

  /**
   * Dois repositorios pequenos podem somar exatamente os mesmos bytes.
   *
   * <p>Sem o desempate, a legenda trocaria de ordem entre duas revalidacoes sem que nada tivesse
   * mudado.
   */
  @Test
  @DisplayName("deve desempatar linguagens pelo nome quando os bytes coincidem")
  void shouldBreakLanguageTies_byName() {
    GitHubStats stats =
        stats(
            List.of(new LanguageUsage("Shell", 1_000), new LanguageUsage("Dockerfile", 1_000)),
            List.of());

    assertThat(stats.languages())
        .extracting(LanguageUsage::name)
        .containsExactly("Dockerfile", "Shell");
  }

  /**
   * "Java" e "JAVA" seriam a mesma linguagem contada duas vezes.
   *
   * <p>O grafico somaria 100% com uma fatia sobrando, e ninguem veria o defeito no numero.
   */
  @Test
  @DisplayName("deve recusar a mesma linguagem duas vezes, ignorando a caixa")
  void shouldReject_whenLanguageRepeats() {
    List<LanguageUsage> repetidas =
        List.of(new LanguageUsage("Java", 10), new LanguageUsage("JAVA", 20));

    assertThatThrownBy(() -> stats(repetidas, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Linguagem repetida: JAVA");
  }

  @Test
  @DisplayName("deve somar os bytes de todas as linguagens")
  void shouldSum_languageBytes() {
    GitHubStats stats =
        stats(
            List.of(new LanguageUsage("Java", 90_000), new LanguageUsage("Python", 10_000)),
            List.of());

    assertThat(stats.totalLanguageBytes()).isEqualTo(100_000);
    assertThat(stats.languages().getFirst().shareOf(stats.totalLanguageBytes())).isEqualTo(90.0);
  }

  @Test
  @DisplayName("deve ordenar os repositorios pelas estrelas")
  void shouldOrderRepositories_byStars() {
    GitHubStats stats =
        stats(
            List.of(),
            List.of(
                repositorio("estudos", 0, LocalDate.of(2026, 8, 1)),
                repositorio("portfolio", 7, LocalDate.of(2026, 1, 1))));

    assertThat(stats.repositories())
        .extracting(RepositorySummary::name)
        .containsExactly("portfolio", "estudos");
  }

  /**
   * Um portfolio novo tem quase tudo com zero estrela.
   *
   * <p>Nesse caso o desempate decide a ordem inteira, e ele responde "no que essa pessoa esta
   * trabalhando" em vez de "o que ela criou primeiro".
   */
  @Test
  @DisplayName("deve desempatar repositorios pelo push mais recente")
  void shouldBreakRepositoryTies_byMostRecentPush() {
    GitHubStats stats =
        stats(
            List.of(),
            List.of(
                repositorio("antigo", 0, LocalDate.of(2025, 1, 10)),
                repositorio("recente", 0, LocalDate.of(2026, 8, 10))));

    assertThat(stats.repositories())
        .extracting(RepositorySummary::name)
        .containsExactly("recente", "antigo");
  }

  @Test
  @DisplayName("deve desempatar pelo nome quando estrelas e push coincidem")
  void shouldBreakRepositoryTies_byName() {
    LocalDate mesmoDia = LocalDate.of(2026, 8, 10);
    GitHubStats stats =
        stats(
            List.of(), List.of(repositorio("zeta", 2, mesmoDia), repositorio("alfa", 2, mesmoDia)));

    assertThat(stats.repositories())
        .extracting(RepositorySummary::name)
        .containsExactly("alfa", "zeta");
  }

  @Test
  @DisplayName("deve recusar o mesmo repositorio duas vezes")
  void shouldReject_whenRepositoryRepeats() {
    List<RepositorySummary> repetidos =
        List.of(
            repositorio("portfolio", 1, LocalDate.of(2026, 8, 1)),
            repositorio("Portfolio", 2, LocalDate.of(2026, 8, 2)));

    assertThatThrownBy(() -> stats(List.of(), repetidos))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Repositorio repetido: Portfolio");
  }

  @Test
  @DisplayName("deve cortar os destaques no limite pedido, preservando a ordem")
  void shouldLimit_highlights() {
    GitHubStats stats =
        stats(
            List.of(),
            List.of(
                repositorio("terceiro", 1, LocalDate.of(2026, 1, 1)),
                repositorio("primeiro", 9, LocalDate.of(2026, 1, 1)),
                repositorio("segundo", 5, LocalDate.of(2026, 1, 1))));

    assertThat(stats.highlights(2))
        .extracting(RepositorySummary::name)
        .containsExactly("primeiro", "segundo");
  }

  @Test
  @DisplayName("deve devolver lista vazia quando o limite nao e positivo")
  void shouldReturnEmpty_whenLimitIsNotPositive() {
    GitHubStats stats = stats(List.of(), List.of(repositorio("um", 1, LocalDate.of(2026, 1, 1))));

    assertThat(stats.highlights(0)).isEmpty();
    assertThat(stats.highlights(-1)).isEmpty();
  }

  @Test
  @DisplayName("deve devolver todos quando o limite excede a lista")
  void shouldReturnAll_whenLimitExceedsList() {
    GitHubStats stats = stats(List.of(), List.of(repositorio("um", 1, LocalDate.of(2026, 1, 1))));

    assertThat(stats.highlights(10)).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"-CrySamuel", "CrySamuel-", "Cry--Samuel", "Cry Samuel", "Cry_Samuel"})
  @DisplayName("deve recusar nome de usuario fora da regra do GitHub")
  void shouldReject_whenUsernameIsInvalid(String username) {
    assertThatThrownBy(() -> new GitHubStats(username, 0, 0, List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome de usuario do GitHub invalido");
  }

  @Test
  @DisplayName("deve aceitar hifen simples no meio do nome")
  void shouldAccept_singleHyphenInsideUsername() {
    assertThat(new GitHubStats("Cry-Samuel", 0, 0, List.of(), List.of()).username())
        .isEqualTo("Cry-Samuel");
  }

  @Test
  @DisplayName("deve recusar nome de usuario ausente")
  void shouldReject_whenUsernameIsMissing() {
    assertThatThrownBy(() -> new GitHubStats(null, 0, 0, List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome de usuario do GitHub e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar contagens negativas")
  void shouldReject_whenCountsAreNegative() {
    assertThatThrownBy(() -> new GitHubStats(USUARIO, -1, 0, List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Total de repositorios publicos nao pode ser negativo");

    assertThatThrownBy(() -> new GitHubStats(USUARIO, 0, -1, List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Contribuicoes no ultimo ano nao pode ser negativo");
  }

  @Test
  @DisplayName("deve recusar listas nulas em vez de trata-las como vazias")
  void shouldReject_whenListsAreNull() {
    assertThatThrownBy(() -> new GitHubStats(USUARIO, 0, 0, null, List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Lista de linguagens e obrigatoria");

    assertThatThrownBy(() -> new GitHubStats(USUARIO, 0, 0, List.of(), null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Lista de repositorios e obrigatoria");
  }

  /**
   * O vazio e o ultimo degrau do fallback do ADR-0008.
   *
   * <p>Ele guarda o nome de usuario mesmo sem dado nenhum, porque e ele que a secao usa para montar
   * o link do perfil - um vazio sem nome obrigaria a tela a ter dois caminhos de renderizacao.
   */
  @Test
  @DisplayName("deve construir o retrato vazio preservando o nome de usuario")
  void shouldBuild_emptyStats() {
    GitHubStats vazio = GitHubStats.empty(USUARIO);

    assertThat(vazio.isEmpty()).isTrue();
    assertThat(vazio.username()).isEqualTo(USUARIO);
    assertThat(vazio.languages()).isEmpty();
    assertThat(vazio.repositories()).isEmpty();
    assertThat(vazio.totalLanguageBytes()).isZero();
    assertThat(vazio.highlights(5)).isEmpty();
  }

  @Test
  @DisplayName("nao deve se declarar vazio quando ha qualquer numero a mostrar")
  void shouldNotBeEmpty_whenThereIsAnythingToShow() {
    assertThat(new GitHubStats(USUARIO, 17, 0, List.of(), List.of()).isEmpty()).isFalse();
    assertThat(new GitHubStats(USUARIO, 0, 240, List.of(), List.of()).isEmpty()).isFalse();
    assertThat(stats(List.of(new LanguageUsage("Java", 1)), List.of()).isEmpty()).isFalse();
  }

  private static GitHubStats stats(
      List<LanguageUsage> languages, List<RepositorySummary> repositories) {
    return new GitHubStats(USUARIO, 17, 240, languages, repositories);
  }

  private static RepositorySummary repositorio(String name, int stars, LocalDate push) {
    return new RepositorySummary(
        name, null, "https://github.com/CrySamuel/" + name, null, stars, push);
  }
}
