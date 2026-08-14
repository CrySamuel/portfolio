package dev.crystofer.portfolio.projects.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import dev.crystofer.portfolio.shared.domain.Slug;

class TechnologyTest {

  @Test
  @DisplayName("deve aceitar tecnologia completa")
  void shouldAccept_whenEverythingIsValid() {
    // when
    Technology tecnologia =
        new Technology(
            "Spring Boot", Slug.of("spring-boot"), TechnologyCategory.FRAMEWORK, "spring");

    // then
    assertThat(tecnologia.name()).isEqualTo("Spring Boot");
    assertThat(tecnologia.slug()).isEqualTo(Slug.of("spring-boot"));
    assertThat(tecnologia.category().code()).isEqualTo("framework");
    assertThat(tecnologia.findIconSlug()).contains("spring");
  }

  /**
   * O nome guarda a capitalizacao da marca, e o slug nao e derivado dele.
   *
   * <p>Se um dia alguem trocar os dois campos por um so, este teste e o que reprova: "Spring Boot"
   * nao e um slug valido, e "spring-boot" nao e como a marca se escreve.
   */
  @Test
  @DisplayName("deve guardar nome e slug como campos independentes")
  void shouldKeep_nameAndSlugApart() {
    Technology tecnologia =
        new Technology("PostgreSQL", Slug.of("postgresql"), TechnologyCategory.DATABASE, null);

    assertThat(tecnologia.name()).isEqualTo("PostgreSQL");
    assertThat(tecnologia.slug().value()).isEqualTo("postgresql");
  }

  @Test
  @DisplayName("deve aceitar tecnologia sem icone")
  void shouldAccept_whenIconIsAbsent() {
    Technology tecnologia =
        new Technology("Docker", Slug.of("docker"), TechnologyCategory.INFRASTRUCTURE, null);

    assertThat(tecnologia.iconSlug()).isNull();
    assertThat(tecnologia.findIconSlug()).isEmpty();
  }

  /**
   * Vazio e recusado em vez de tratado como ausente.
   *
   * <p>Se string vazia virasse {@code null} em silencio, haveria dois jeitos de dizer "sem icone" e
   * o mapeamento teria de tratar os dois em todo lugar. Basta um esquecido para a tela pedir ao
   * sprite um simbolo de nome vazio.
   */
  @ParameterizedTest
  @DisplayName("deve recusar icone em branco")
  @ValueSource(strings = {"", "   "})
  void shouldReject_whenIconIsBlank(String branco) {
    assertThatThrownBy(
            () -> new Technology("Docker", Slug.of("docker"), TechnologyCategory.TOOL, branco))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("use null");
  }

  @ParameterizedTest
  @DisplayName("deve recusar nome vazio ou nulo")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void shouldReject_whenNameIsBlank(String invalido) {
    assertThatThrownBy(
            () -> new Technology(invalido, Slug.of("java"), TechnologyCategory.LANGUAGE, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nome da tecnologia");
  }

  @Test
  @DisplayName("deve recusar tecnologia sem slug")
  void shouldReject_whenSlugIsNull() {
    assertThatThrownBy(() -> new Technology("Java", null, TechnologyCategory.LANGUAGE, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Slug da tecnologia");
  }

  @Test
  @DisplayName("deve recusar tecnologia sem categoria")
  void shouldReject_whenCategoryIsNull() {
    assertThatThrownBy(() -> new Technology("Java", Slug.of("java"), null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Categoria da tecnologia");
  }

  @Test
  @DisplayName("deve remover espaco nas pontas do nome")
  void shouldTrim_theName() {
    Technology tecnologia =
        new Technology("  Java  ", Slug.of("java"), TechnologyCategory.LANGUAGE, null);

    assertThat(tecnologia.name()).isEqualTo("Java");
  }

  @Test
  @DisplayName("deve recusar nome acima do limite da coluna")
  void shouldReject_whenNameExceedsTheColumn() {
    String longo = "a".repeat(61);

    assertThatThrownBy(
            () -> new Technology(longo, Slug.of("java"), TechnologyCategory.LANGUAGE, null))
        .hasMessageContaining("excede 60");
  }

  /**
   * Os cinco codigos batem com o {@code technology_category_ck} da migracao.
   *
   * <p>Sao duas listas, em arquivos diferentes, e nada alem deste teste as compara. Acrescentar uma
   * constante aqui sem acrescentar ao {@code CHECK} produziria um valor que o dominio aceita e o
   * banco recusa - erro de driver no seed, longe da causa.
   */
  @Test
  @DisplayName("deve publicar exatamente os cinco codigos da migracao")
  void shouldPublish_theFiveCodesOfTheMigration() {
    assertThat(TechnologyCategory.values())
        .extracting(TechnologyCategory::code)
        .containsExactlyInAnyOrder("language", "framework", "database", "infrastructure", "tool");
  }
}
