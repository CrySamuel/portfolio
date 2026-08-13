package dev.crystofer.portfolio.profile.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A ordem <em>entre</em> categorias, e o que o catalogo recusa deixar passar. */
class SkillCatalogTest {

  @Test
  @DisplayName("deve ordenar as categorias pela ordem editorial")
  void shouldOrderByDisplayOrder_whenInputIsShuffled() {
    // given
    var linguagens = umaCategoria("Linguagens", 0);
    var bancos = umaCategoria("Bancos de Dados", 1);
    var infra = umaCategoria("Infraestrutura", 2);

    // when
    var catalogo = new SkillCatalog(List.of(infra, linguagens, bancos));

    // then
    assertThat(catalogo.categories())
        .extracting(SkillCategory::name)
        .containsExactly("Linguagens", "Bancos de Dados", "Infraestrutura");
  }

  /**
   * O desempate importa porque o seed nao impede numeros repetidos.
   *
   * <p>Sem ele, duas categorias com o mesmo {@code displayOrder} sairiam em ordem indefinida.
   */
  @Test
  @DisplayName("deve desempatar por nome quando a ordem editorial coincide")
  void shouldFallBackToName_whenDisplayOrderTies() {
    // given
    var zeta = umaCategoria("Zeta", 5);
    var alfa = umaCategoria("Alfa", 5);

    // when
    var catalogo = new SkillCatalog(List.of(zeta, alfa));

    // then
    assertThat(catalogo.categories())
        .extracting(SkillCategory::name)
        .containsExactly("Alfa", "Zeta");
  }

  /**
   * Cabecalho sem competencia abaixo nao vai para a tela.
   *
   * <p>Seria ruido visual e um item a mais para o leitor de tela anunciar sem conteudo. A decisao
   * mora aqui, e nao no componente, para nao existir em dois lugares.
   */
  @Test
  @DisplayName("deve descartar categoria sem competencias")
  void shouldDropCategory_whenItHasNoSkills() {
    // given
    var comSkills = umaCategoria("Linguagens", 0);
    var vazia = new SkillCategory("Vazia", 1, List.of());

    // when
    var catalogo = new SkillCatalog(List.of(comSkills, vazia));

    // then
    assertThat(catalogo.categories()).extracting(SkillCategory::name).containsExactly("Linguagens");
  }

  @Test
  @DisplayName("deve recusar categoria repetida")
  void shouldReject_whenCategoryNameRepeats() {
    // when
    var thrown =
        catchThrowable(
            () ->
                new SkillCatalog(
                    List.of(umaCategoria("Linguagens", 0), umaCategoria("Linguagens", 1))));

    // then
    assertThat(thrown).hasMessageContaining("Categoria repetida");
  }

  @Test
  @DisplayName("deve recusar lista ausente")
  void shouldReject_whenListIsNull() {
    // when
    var thrown = catchThrowable(() -> new SkillCatalog(null));

    // then
    assertThat(thrown).hasMessageContaining("Lista de categorias e obrigatoria");
  }

  @Test
  @DisplayName("deve tratar catalogo sem categorias como estado legitimo")
  void shouldBeEmpty_whenThereAreNoCategories() {
    // when
    var catalogo = SkillCatalog.empty();

    // then
    assertThat(catalogo.isEmpty()).isTrue();
    assertThat(catalogo.totalSkills()).isZero();
  }

  @Test
  @DisplayName("deve contar as competencias somando as categorias")
  void shouldCountSkillsAcrossCategories() {
    // given
    var linguagens =
        new SkillCategory(
            "Linguagens",
            0,
            List.of(
                new Skill("Java", Proficiency.ADVANCED, 3),
                new Skill("Python", Proficiency.INTERMEDIATE, 2)));
    var bancos = umaCategoria("Bancos de Dados", 1);

    // when
    var catalogo = new SkillCatalog(List.of(linguagens, bancos));

    // then
    assertThat(catalogo.totalSkills()).isEqualTo(3);
  }

  private static SkillCategory umaCategoria(String nome, int ordem) {
    return new SkillCategory(nome, ordem, List.of(new Skill("Uma", Proficiency.BASIC, null)));
  }
}
