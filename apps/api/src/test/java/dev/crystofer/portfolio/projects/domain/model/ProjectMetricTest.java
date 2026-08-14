package dev.crystofer.portfolio.projects.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ProjectMetricTest {

  @Test
  @DisplayName("deve aceitar metrica completa")
  void shouldAccept_whenEverythingIsValid() {
    ProjectMetric metrica = new ProjectMetric("p95", "80ms", 0);

    assertThat(metrica.label()).isEqualTo("p95");
    assertThat(metrica.value()).isEqualTo("80ms");
    assertThat(metrica.displayOrder()).isZero();
  }

  /**
   * As formas reais que o valor assume, e a razao de a coluna ser texto.
   *
   * <p>Nenhuma delas cabe num tipo numerico sem uma coluna de unidade ao lado, e a ultima nao cabe
   * nem com ela. Como conta nenhuma e feita sobre esses valores, texto e a representacao honesta.
   */
  @ParameterizedTest
  @DisplayName("deve aceitar valor com unidade em qualquer forma")
  @ValueSource(strings = {"80ms", "40%", "R$ 800+", "4h para 2h", "24/7", "0"})
  void shouldAccept_whenValueCarriesItsUnit(String valor) {
    assertThat(new ProjectMetric("medida", valor, 0).value()).isEqualTo(valor);
  }

  @ParameterizedTest
  @DisplayName("deve recusar rotulo vazio ou nulo")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldReject_whenLabelIsBlank(String invalido) {
    assertThatThrownBy(() -> new ProjectMetric(invalido, "80ms", 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Rotulo da metrica");
  }

  @ParameterizedTest
  @DisplayName("deve recusar valor vazio ou nulo")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldReject_whenValueIsBlank(String invalido) {
    assertThatThrownBy(() -> new ProjectMetric("p95", invalido, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Valor da metrica");
  }

  @Test
  @DisplayName("deve remover espaco nas pontas dos dois campos")
  void shouldTrim_bothFields() {
    ProjectMetric metrica = new ProjectMetric("  p95  ", "  80ms  ", 1);

    assertThat(metrica.label()).isEqualTo("p95");
    assertThat(metrica.value()).isEqualTo("80ms");
  }

  @Test
  @DisplayName("deve recusar campos acima dos limites das colunas")
  void shouldReject_whenFieldsExceedTheColumns() {
    assertThatThrownBy(() -> new ProjectMetric("a".repeat(61), "80ms", 0))
        .hasMessageContaining("excede 60");
    assertThatThrownBy(() -> new ProjectMetric("p95", "a".repeat(41), 0))
        .hasMessageContaining("excede 40");
  }
}
