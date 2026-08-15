package dev.crystofer.portfolio.github.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LanguageUsageTest {

  @Test
  @DisplayName("deve aparar espacos do nome")
  void shouldTrim_name() {
    assertThat(new LanguageUsage("  Java  ", 1_024).name()).isEqualTo("Java");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  @DisplayName("deve recusar nome vazio ou em branco")
  void shouldReject_whenNameIsBlank(String name) {
    assertThatThrownBy(() -> new LanguageUsage(name, 1_024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome da linguagem e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar nome nulo")
  void shouldReject_whenNameIsNull() {
    assertThatThrownBy(() -> new LanguageUsage(null, 1_024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome da linguagem e obrigatorio");
  }

  @Test
  @DisplayName("deve recusar nome mais longo que o limite")
  void shouldReject_whenNameIsTooLong() {
    assertThatThrownBy(() -> new LanguageUsage("L".repeat(61), 1_024))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("excede 60 caracteres");
  }

  /**
   * Zero bytes nao e pouco uso, e ausencia.
   *
   * <p>O GitHub nao lista a linguagem nesse caso. Aceitar zero deixaria entrar uma fatia invisivel
   * no grafico que ainda assim ocuparia uma legenda.
   */
  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  @DisplayName("deve recusar bytes nao positivos")
  void shouldReject_whenBytesAreNotPositive(long bytes) {
    assertThatThrownBy(() -> new LanguageUsage("Java", bytes))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Bytes da linguagem precisam ser positivos");
  }

  @Test
  @DisplayName("deve calcular a fatia sobre o total")
  void shouldCompute_shareOfTotal() {
    LanguageUsage java = new LanguageUsage("Java", 750);

    assertThat(java.shareOf(1_000)).isCloseTo(75.0, within(0.001));
  }

  /**
   * A fatia nao arredonda de proposito.
   *
   * <p>Arredondar e decisao de apresentacao, e uma que nao fecha em 100% sozinha - quem desenha o
   * grafico precisa escolher onde absorver a diferenca, e o dominio nao deve escolher por ele.
   */
  @Test
  @DisplayName("deve devolver a fatia sem arredondar")
  void shouldNotRound_share() {
    LanguageUsage python = new LanguageUsage("Python", 1);

    assertThat(python.shareOf(3)).isCloseTo(33.333, within(0.001));
  }

  /** Total zero acontece quando nao ha linguagem nenhuma - dividir ali seria erro, nao zero. */
  @Test
  @DisplayName("deve devolver zero quando o total e zero")
  void shouldReturnZero_whenTotalIsZero() {
    assertThat(new LanguageUsage("Java", 100).shareOf(0)).isZero();
  }
}
