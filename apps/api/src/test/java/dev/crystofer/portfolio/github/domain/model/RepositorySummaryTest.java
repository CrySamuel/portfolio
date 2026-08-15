package dev.crystofer.portfolio.github.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RepositorySummaryTest {

  private static final LocalDate PUSH = LocalDate.of(2026, 4, 15);

  @Test
  @DisplayName("deve aparar espacos dos textos")
  void shouldTrim_texts() {
    RepositorySummary repositorio =
        new RepositorySummary(
            "  FinAI-Bot  ",
            "  Assistente financeiro  ",
            "  https://github.com/CrySamuel/FinAI-Bot  ",
            "  Python  ",
            3,
            PUSH);

    assertThat(repositorio.name()).isEqualTo("FinAI-Bot");
    assertThat(repositorio.description()).isEqualTo("Assistente financeiro");
    assertThat(repositorio.url()).isEqualTo("https://github.com/CrySamuel/FinAI-Bot");
    assertThat(repositorio.primaryLanguage()).isEqualTo("Python");
  }

  @Test
  @DisplayName("deve recusar nome ausente")
  void shouldReject_whenNameIsMissing() {
    assertThatThrownBy(() -> repositorio(null, "https://github.com/u/r", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome do repositorio e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar endereco ausente")
  void shouldReject_whenUrlIsMissing() {
    assertThatThrownBy(() -> repositorio("portfolio", null, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Endereco do repositorio e obrigatorio");
  }

  /**
   * Endereco sem esquema e o erro que nao aparece.
   *
   * <p>{@code github.com/user/repo} num href vira caminho relativo, o navegador resolve contra o
   * proprio site e devolve 404 - sem erro no console e sem linha no log. E a mesma guarda que
   * {@code Project} tem para repositorio e site.
   */
  @Test
  @DisplayName("deve recusar endereco sem https")
  void shouldReject_whenUrlHasNoScheme() {
    assertThatThrownBy(() -> repositorio("portfolio", "github.com/CrySamuel/portfolio", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("precisa comecar com https://");
  }

  @Test
  @DisplayName("deve recusar http em texto claro")
  void shouldReject_whenUrlIsPlainHttp() {
    assertThatThrownBy(() -> repositorio("portfolio", "http://github.com/CrySamuel/portfolio", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("precisa comecar com https://");
  }

  /** Repositorio sem descricao e comum; sem linguagem detectada, tambem. */
  @Test
  @DisplayName("deve aceitar descricao e linguagem ausentes")
  void shouldAccept_whenOptionalFieldsAreNull() {
    RepositorySummary repositorio =
        new RepositorySummary("dotfiles", null, "https://github.com/u/dotfiles", null, 0, PUSH);

    assertThat(repositorio.findDescription()).isEmpty();
    assertThat(repositorio.findPrimaryLanguage()).isEmpty();
  }

  /**
   * Ausente e valido; em branco nao.
   *
   * <p>Com dois jeitos de representar ausencia, metade do codigo confere um e metade confere o
   * outro.
   */
  @Test
  @DisplayName("deve recusar descricao em branco em vez de trata-la como ausente")
  void shouldReject_whenDescriptionIsBlank() {
    assertThatThrownBy(
            () ->
                new RepositorySummary(
                    "dotfiles", "   ", "https://github.com/u/dotfiles", null, 0, PUSH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("use null quando nao houver");
  }

  @Test
  @DisplayName("deve devolver descricao e linguagem quando existem")
  void shouldReturn_optionalFields() {
    RepositorySummary repositorio =
        new RepositorySummary(
            "portfolio", "Monorepo", "https://github.com/u/portfolio", "Java", 1, PUSH);

    assertThat(repositorio.findDescription()).contains("Monorepo");
    assertThat(repositorio.findPrimaryLanguage()).contains("Java");
  }

  @Test
  @DisplayName("deve recusar estrelas negativas")
  void shouldReject_whenStarsAreNegative() {
    assertThatThrownBy(() -> repositorio("portfolio", "https://github.com/u/portfolio", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Estrelas nao podem ser negativas");
  }

  /**
   * A data e exigida porque e por ela que os repositorios sao desempatados.
   *
   * <p>Ordenacao que dependa de campo opcional passa a ter dois comportamentos. Se a API do GitHub
   * um dia devolver nulo, o adaptador falha e o fallback do ADR-0008 entrega o cache - que e o
   * comportamento certo para resposta malformada.
   */
  @Test
  @DisplayName("deve recusar data de push ausente")
  void shouldReject_whenPushDateIsMissing() {
    assertThatThrownBy(
            () ->
                new RepositorySummary(
                    "portfolio", null, "https://github.com/u/portfolio", null, 0, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Data do ultimo push e obrigatoria");
  }

  private static RepositorySummary repositorio(String name, String url, int stars) {
    return new RepositorySummary(name, null, url, null, stars, PUSH);
  }
}
