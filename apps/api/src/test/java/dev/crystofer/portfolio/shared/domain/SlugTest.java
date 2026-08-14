package dev.crystofer.portfolio.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SlugTest {

  @ParameterizedTest
  @DisplayName("deve aceitar slug em kebab-case")
  @ValueSource(strings = {"finai", "portfolio", "music-style-api", "api-v2", "a", "123", "a-1-b"})
  void shouldAccept_whenFormatIsValid(String valido) {
    assertThat(Slug.of(valido).value()).isEqualTo(valido);
  }

  @ParameterizedTest
  @DisplayName("deve recusar slug vazio ou nulo")
  @NullAndEmptySource
  @ValueSource(strings = {"   ", "\t"})
  void shouldReject_whenBlank(String invalido) {
    assertThatThrownBy(() -> Slug.of(invalido))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("obrigatorio");
  }

  /**
   * Cada caso e um erro real, e nao uma variacao aleatoria.
   *
   * <p>{@code Music Style API} e o titulo copiado sem transformar. {@code MUSIC} e a maiuscula que
   * criaria dois enderecos para a mesma pagina. {@code music_style} e a convencao de banco de dados
   * numa URL. Os tres seguintes - hifen duplicado, no fim e no inicio - sao o que geracao
   * automatica produz, e sao a razao de o padrao usar grupo repetido em vez de {@code [a-z0-9-]+}.
   * O ultimo tem acentuacao, que vira escape percentual justamente onde o slug existe para ser
   * lido.
   */
  @ParameterizedTest
  @DisplayName("deve recusar slug fora do formato da URL")
  @ValueSource(
      strings = {
        "Music Style API",
        "MUSIC",
        "music_style",
        "music--style",
        "music-",
        "-music",
        "cafe-com-acentuacao-ha-aqui-ç",
        "com.ponto",
        "com/barra"
      })
  void shouldReject_whenFormatIsInvalid(String invalido) {
    assertThatThrownBy(() -> Slug.of(invalido))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("formato");
  }

  @Test
  @DisplayName("deve remover espaco nas pontas antes de validar")
  void shouldTrim_whenSurroundedBySpaces() {
    assertThat(Slug.of("  finai  ").value()).isEqualTo("finai");
  }

  /** O limite espelha {@code project.slug VARCHAR(80)} - o de dentro passa, o de fora nao. */
  @Test
  @DisplayName("deve aceitar exatamente 80 caracteres e recusar 81")
  void shouldRespect_theColumnLimit() {
    String oitenta = "a".repeat(80);

    assertThat(Slug.of(oitenta).value()).hasSize(80);
    assertThatThrownBy(() -> Slug.of("a".repeat(81))).hasMessageContaining("excede 80");
  }

  /**
   * O {@code toString} devolve o valor, e ha teste porque a linha preguicosa depende disso.
   *
   * <p>Com o {@code toString} gerado por record, {@code "/projetos/" + slug} montaria {@code
   * /projetos/Slug[value=finai]} - compila, e so aparece quando alguem clica.
   */
  @Test
  @DisplayName("deve concatenar como o proprio valor")
  void shouldStringify_asTheRawValue() {
    Slug slug = Slug.of("finai");

    assertThat("/projetos/" + slug).isEqualTo("/projetos/finai");
    assertThat(slug.toString()).isEqualTo("finai");
  }

  @Test
  @DisplayName("deve comparar por valor, e nao por identidade")
  void shouldCompare_byValue() {
    assertThat(Slug.of("finai")).isEqualTo(Slug.of("finai")).isNotEqualTo(Slug.of("portfolio"));
    assertThat(Slug.of("finai")).hasSameHashCodeAs(Slug.of("finai"));
  }
}
